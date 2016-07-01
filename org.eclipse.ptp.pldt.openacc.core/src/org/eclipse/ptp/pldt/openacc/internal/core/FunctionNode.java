/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.internal.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccGangClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccVectorClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccWorkerClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;

/**
 * Used to create and analyze a function call graph.
 * 
 * <p>When performing the IntroRoutine refactoring, all definitions called in refactored functions must also be 
 * refactored. Each node in the graph contains a function definition and a list of children, or nodes containing 
 * definitions of functions called in the current node's definition. When created, a node will add itself to its 
 * parent's children, create or find its own children, and then determine its current level.</p>
 * 
 * <p>A function's level determines the clause in the pragma directive, and is either sequential, vector, worker, 
 * or gang, with each level higher than the last. Each node must have a level higher than the highest level of all its children. The exception is if
 * the highest level is sequential, in which case the node may itself also be sequential. If a node contains a child
 * with the gang level, then the function cannot be parallelized and an exception is thrown.</p>
 *
 * <p>Every node is sequential by default, with a higher level forced only if a child has a higher level or there
 * is an inherent level in the definition, i.e., it contains a parallel 
 * region. Such a node must have at least the level specified by the parallel region. The entire graph will be 
 * sequential unless one of these parallel regions is encountered. Then the parallelism will propagate up the call
 * hierarchy.</p>
 */
public class FunctionNode {

	private final FunctionNode root;
	private final RefactoringStatus status;
	private FunctionLevel level = null;
	private final IASTFunctionDefinition definition;
	private final ArrayList<FunctionNode> children = new ArrayList<FunctionNode>();
	
	

	//Used to create root of the tree, which may contain multiple unrelated definitions as children.
	public FunctionNode(List<IASTFunctionDefinition> definitions, RefactoringStatus status) 
			throws FunctionGraphException {
		this.definition = null;
		this.root = this;
		this.status = status;
		for (IASTFunctionDefinition definition : definitions) {
			if (ASTUtil.getPragmaNodes(definition).isEmpty()) {
				FunctionNode existingNode = root.findDescendant(definition, new HashSet<FunctionNode>());
				if (existingNode != null && (!children.contains(existingNode))) {
					this.children.add(existingNode);
				} else if (existingNode == null) {
					new FunctionNode(definition, root, this);
				}
			}
		}
	}
	
	private FunctionNode(IASTFunctionDefinition definition, FunctionNode root, FunctionNode parent) 
			throws FunctionGraphException {
		this.definition = definition;
		this.root = root;
		this.status = null;
		parent.children.add(this); // Children have to be added in constructor, not after, to avoid infinite loop.
		this.checkForStatic();
		this.addChildren();
		this.findInherentRestriction();
		this.findCurrentLevel();
	}
	
	public ArrayList<FunctionNode> getChildren() {
		return children;
	}
	
	public IASTFunctionDefinition getDefinition() {
		return definition;
	}
	
	public FunctionLevel getLevel() {
		return level;
	}
	
	private void checkForStatic() {
		for (IASTName name : ASTUtil.find(definition, IASTName.class)) {
			if (name.resolveBinding() instanceof IVariable && ((IVariable) name.resolveBinding()).isStatic()) {
				root.status.addError(Messages.FunctionNode_CannotContainStaticVariables, 
						ASTUtil.getStatusContext(name, name));
			}
		}
	}
	
	private void addChildren() throws FunctionGraphException {
		for (IASTFunctionCallExpression call : ASTUtil.find(definition, IASTFunctionCallExpression.class)) {
			IASTFunctionDefinition functionDefinition = ASTUtil.findFunctionDefinition(call);
			if (functionDefinition == null) {
				root.status.addError(Messages.FunctionNode_CannotFindFunctionDefinitions, ASTUtil.getStatusContext(call, call));
			} else {
				for (IASTPreprocessorPragmaStatement pragma : ASTUtil.getPragmaNodes(functionDefinition)) {
					level = getLevelFromPragma(pragma, level);
				}
				if (level == null) { // Implies the child doesn't have a preceding pragma, and should be modified.
					FunctionNode existingNode = root.findDescendant(functionDefinition, new HashSet<FunctionNode>());
					if (existingNode != null && !children.contains(existingNode)) {
						this.children.add(existingNode);
					} else {
						new FunctionNode(functionDefinition, root, this);
					}
				} else {
					level = level.next(); // Routines must be called from one level higher than themselves.
					if (level == null) {
						throw new FunctionGraphException(Messages.FunctionNode_InconsistentLevelsofParallelism); 
					}
				}
			}
		}
	}
	
	private void findInherentRestriction() throws FunctionGraphException {
		for (IASTPreprocessorPragmaStatement pragma : ASTUtil.getInternalPragmaNodes(definition.getBody())) {
			this.level = getLevelFromPragma(pragma, level);
		}
	}

	private FunctionLevel getLevelFromPragma(IASTPreprocessorPragmaStatement pragma, FunctionLevel level) throws FunctionGraphException {
		try {
			IAccConstruct parse = new OpenACCParser().parse(pragma.getRawSignature());
			if (!OpenACCUtil.find(parse, 
					ASTAccVectorClauseNode.class).isEmpty() && 
					(level == null || level == FunctionLevel.SEQ)) {
				level = FunctionLevel.VECTOR;
			} else if (!OpenACCUtil.find(parse, 
					ASTAccWorkerClauseNode.class).isEmpty() && 
					(level == null || level.compareTo(FunctionLevel.WORKER) < 0)) {
				level = FunctionLevel.WORKER;
			} else if (OpenACCUtil.isAccAccelConstruct(parse)
					|| !OpenACCUtil.find(parse, 
							ASTAccGangClauseNode.class).isEmpty()) {
				level = FunctionLevel.GANG;
			} 
		} catch (Exception e) {
			root.status.addError(Messages.FunctionNode_CannotParsePreprocessorStatement, 
					ASTUtil.getStatusContext(pragma, pragma));
		}
		return level;
	}
	
	private void findCurrentLevel() throws FunctionGraphException {
		if (this.level == null) {
			this.level = FunctionLevel.SEQ;
		} 
		for (FunctionNode child : children) {
			if (child.level.compareTo(level) >= 0) {
				level = child.level.next();
			}
			if (level == null) {
				throw new FunctionGraphException(Messages.FunctionNode_InconsistentLevelsofParallelism);
			}
		}
		for (IASTFunctionCallExpression call : ASTUtil.findFunctionCalls(definition)) {
			if (definition.equals(ASTUtil.findFunctionDefinition(call))) {
				for (IASTNode node = (IASTNode) ASTUtil.findNearestAncestor(call, IASTStatement.class); 
						node != null && !(node instanceof IASTTranslationUnit); node = node.getParent()) {
					for (IASTPreprocessorPragmaStatement pragma : ASTUtil.getPragmaNodes(node)) {
						if ((level.compareTo(FunctionLevel.SEQ) != 0)
								&& (level.compareTo(getLevelFromPragma(pragma, level)) >= 0)) {
							throw new FunctionGraphException(Messages.FunctionNode_InconsistentCallLevels, 
									ASTUtil.getStatusContext(pragma, call));
						}
					}
				}
			}
		}
		HashSet<FunctionNode> visited = new HashSet<FunctionNode>();
		if (level != FunctionLevel.SEQ && this.findDescendant(definition, visited) != null) {
			root.status.addError(Messages.FunctionNode_RecursiveMustBeSequential,
					ASTUtil.getStatusContext(definition, definition));
		}
	}
	
	private FunctionNode findDescendant(IASTFunctionDefinition definition, HashSet<FunctionNode> visited) {
		for (FunctionNode child : children) {
			if (child.definition != null && child.definition.equals(definition)) {
				return child;
			} else if (!visited.contains(child)) {
				visited.add(child);
				FunctionNode output = child.findDescendant(definition, visited);
				if (output != null) {
					return output;
				}
			}
		}
		return null;
	}
	
	
	
	@Override
	public String toString() {
		String output = ""; //$NON-NLS-1$
		if (root.equals(this)) {
			output += Messages.FunctionNode_Root;
		}
		output += Messages.FunctionNode_Level;
		if (level != null) {
			output += level.name();
		} else {
			output += Messages.FunctionNode_Null;
		}
		output += Messages.FunctionNode_Children;
		output += children.size();
		return output;
	}
}
