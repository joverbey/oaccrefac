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
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;

public class FunctionNode {

	private FunctionNode root;
	private FunctionLevel level = null;
	private IASTFunctionDefinition definition;
	private ArrayList<FunctionNode> children = new ArrayList<FunctionNode>();
	
	

	//Used to create root of the tree, which may contain multiple unrelated definitions as children.
	public FunctionNode(List<IASTFunctionDefinition> definitions) throws FunctionGraphException {
		this.definition = null;
		this.root = this;
		for (IASTFunctionDefinition definition : definitions) {
			new FunctionNode(definition, root, this);
		}
	}
	
	private FunctionNode(IASTFunctionDefinition definition, FunctionNode root, FunctionNode parent) 
			throws FunctionGraphException {
		this.definition = definition;
		this.root = root;
		parent.children.add(this); // Children have to be added in constructor, not after, to avoid infinite loop.
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
	
	private void addChildren() throws FunctionGraphException {
		for (IASTFunctionCallExpression call : ASTUtil.find(definition, IASTFunctionCallExpression.class)) {
			IASTFunctionDefinition functionDefinition = ASTUtil.findFunctionDefinition(call);
			for (IASTPreprocessorPragmaStatement pragma : ASTUtil.getPragmaNodes(functionDefinition)) {
				setLevelFromPragma(pragma.getRawSignature());
			}
			if (level == null) { // Implies the child doesn't have a preceding pragma, and should be modified.
				FunctionNode existingNode = root.findDescendent(functionDefinition, new HashSet<FunctionNode>());
				if (existingNode != null) {
					this.children.add(existingNode);
				} else {
					new FunctionNode(functionDefinition, root, this);
				}
			} else {
				level = level.next(); // Routines must be called from one level higher than themselves.
				if (level == null) {
					throw new FunctionGraphException("Levels of parallelism in the function call graph are inconsistent."); 
				}
			}
		}
	}
	
	private void findInherentRestriction() {
		for (IASTPreprocessorPragmaStatement pragma : ASTUtil.getInternalPragmaNodes(definition.getBody())) {
			setLevelFromPragma(pragma.getRawSignature());
		}
	}

	private void setLevelFromPragma(String rawSignature) {
		if (rawSignature.contains("parallel") || rawSignature.contains("kernels")
				|| rawSignature.contains("gang")) {
			level = FunctionLevel.GANG;
		} else if (rawSignature.contains("seq") && (level == null)) {
			level = FunctionLevel.SEQ;
		} else if (rawSignature.contains("vector") && (level == null || level == FunctionLevel.SEQ)) {
			level = FunctionLevel.VECTOR;
		} else if (rawSignature.contains("worker") && (level == null || level.compareTo(FunctionLevel.WORKER) < 0)) {
			level = FunctionLevel.WORKER;
		} 
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
				throw new FunctionGraphException("Levels of parallelism in the function call graph are inconsistent.");
			}
		}
		HashSet<FunctionNode> visited = new HashSet<FunctionNode>();
		if (level != FunctionLevel.SEQ && this.findDescendent(definition, visited) != null) {
			throw new FunctionGraphException("Recursive functions must have sequential parallelization.");
		}
	}
	
	private FunctionNode findDescendent(IASTFunctionDefinition definition, HashSet<FunctionNode> visited) {
		for (FunctionNode child : children) {
			if (child.definition != null && child.definition.equals(definition)) {
				return child;
			} else if (!visited.contains(child)) {
				visited.add(child);
				FunctionNode output = child.findDescendent(definition, visited);
				if (output != null) {
					return output;
				}
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		String output = "";
		if (root.equals(this)) {
			output += "Root, ";
		}
		output += "Level: ";
		if (level != null) {
			output += level.name();
		} else {
			output += "null";
		}
		output += ", Children: ";
		output += children.size();
		return output;
	}
}
