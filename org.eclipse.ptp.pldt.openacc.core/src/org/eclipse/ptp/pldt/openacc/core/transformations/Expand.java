/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Auburn University - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitionsAnalysis;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyinClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyoutClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCreateClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataItemNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTVisitor;
import org.eclipse.ptp.pldt.openacc.core.parser.IASTListNode;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;

public class Expand extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

	public Expand(IASTRewrite rewriter, ExpandDataConstructCheck check) {
		super(rewriter, check);
	}
	
	@Override
	public void doChange() {
		Expansion largestexp = determineLargestExpansion();

		this.remove(getPragma());

		IASTStatement[] exparr = largestexp.getStatements();
		if(getStatement() instanceof IASTCompoundStatement) {
			int stmtOffset = getStatement().getFileLocation().getNodeOffset();
			String comp = getStatement().getRawSignature();
			this.remove(stmtOffset + comp.indexOf('{'), 1);
			this.remove(stmtOffset + comp.lastIndexOf('}'), 1);
		}
		
		this.insertBefore(exparr[0], "{" + NL);
		
		List<IASTComment> commentsWithPragma = new ArrayList<IASTComment>();
		for(IASTComment comment : getStatement().getTranslationUnit().getComments()) {
			if(comment.getFileLocation().getStartingLineNumber() == getPragma().getFileLocation().getStartingLineNumber()) {
				commentsWithPragma.add(comment);
				this.remove(comment);
			}
		}
		insertNewPragma(exparr, commentsWithPragma);
		
		this.insertAfter(exparr[exparr.length - 1], NL + "}");
		
		finalizeChanges();
	}

	private Expansion determineLargestExpansion() {
		int maxup = getMaxInDirection(getStatement(), true);
		int maxdown = getMaxInDirection(getStatement(), false);
		int osize = ASTUtil.getStatementsIfCompound(getStatement()).length;
		List<Expansion> expansions = new ArrayList<Expansion>();

		for (int i = 0; i <= maxup; i++) {
			for (int j = 0; j <= maxdown; j++) {
				IASTStatement[] expStmts = getExpansionStatements(i, j, getStatement());
				// be sure we don't add a declaration to this inner scope if it is used in the outer scope
				if (ASTUtil.doesConstructContainAllReferencesToVariablesItDeclares(expStmts))
					expansions.add(new Expansion(expStmts, osize + i + j));
			}
		}

		Expansion largestexp = null;
		for (Expansion exp : expansions) {
			if (largestexp == null || exp.getSize() >= largestexp.getSize()) {
				largestexp = exp;
			}
		}
		return largestexp;
	}
	
	private void insertNewPragma(IASTStatement[] exparr, List<IASTComment> comments) {

		class DeclaratorRemover extends ASTVisitor {

			private String declarator;

			public DeclaratorRemover(IASTDeclarator declarator) {
				this.declarator = declarator.getName().toString();
			}

			private boolean empty(IASTListNode<ASTAccDataItemNode> items) {
				for(ASTAccDataItemNode item : items) {
					if(item != null) {
						return false;
					}
				}
				return true;
			}
			
			@Override
			public void visitASTAccCopyinClauseNode(ASTAccCopyinClauseNode node) {
				IASTListNode<ASTAccDataItemNode> list = node.getAccDataList();
				for (int i = 0; i < list.size(); i++) {
					ASTAccDataItemNode item = list.get(i);
					if(item != null) {
						if (item.getIdentifier().getIdentifier().getText().equals(declarator)) {
							getCheck().getStatus().addInfo(String.format("Identifier \"%s\" will be removed from copyin clause", declarator));
							list.remove(i);
						}
					}
				}
				if(empty(node.getAccDataList())) node.removeFromTree();
				traverseChildren(node);
			}

			@Override
			public void visitASTAccCopyoutClauseNode(ASTAccCopyoutClauseNode node) {
				IASTListNode<ASTAccDataItemNode> list = node.getAccDataList();
				for (int i = 0; i < list.size(); i++) {
					ASTAccDataItemNode item = list.get(i);
					if(item != null) {
						if (item.getIdentifier().getIdentifier().getText().equals(declarator)) {
							getCheck().getStatus().addInfo(String.format("Identifier \"%s\" will be removed from copyout clause", declarator));
							list.remove(i);
						}
					}
				}
				if(empty(node.getAccDataList())) node.removeFromTree();
				traverseChildren(node);
			}

			@Override
			public void visitASTAccCopyClauseNode(ASTAccCopyClauseNode node) {
				IASTListNode<ASTAccDataItemNode> list = node.getAccDataList();
				for (int i = 0; i < list.size(); i++) {
					ASTAccDataItemNode item = list.get(i);
					if(item != null) {
						if (item.getIdentifier().getIdentifier().getText().equals(declarator)) {
							getCheck().getStatus().addInfo(String.format("Identifier \"%s\" will be removed from copy clause", declarator));
							list.remove(i);
						}
					}
				}
				if(empty(node.getAccDataList())) node.removeFromTree();
				traverseChildren(node);
			}

			@Override
			public void visitASTAccCreateClauseNode(ASTAccCreateClauseNode node) {
				IASTListNode<ASTAccDataItemNode> list = node.getAccDataList();
				for (int i = 0; i < list.size(); i++) {
					ASTAccDataItemNode item = list.get(i);
					if(item != null) {
						if (item.getIdentifier().getIdentifier().getText().equals(declarator)) {
							getCheck().getStatus().addInfo(String.format("Identifier \"%s\" will be removed from create clause", declarator));
							list.remove(i);
						}
					}
				}
				if(empty(node.getAccDataList())) node.removeFromTree();
				traverseChildren(node);
			}
		}

		List<IASTDeclarator> declarators = new ArrayList<IASTDeclarator>();
		for (IASTStatement stmt : exparr) {
			for (IASTDeclarator decl : ASTUtil.find(stmt, IASTDeclarator.class)) {
				if (ASTUtil.findNearestAncestor(decl, IASTSimpleDeclaration.class) != null) {
					declarators.add(decl);
				}
			}
		}
		
		ASTAccDataNode construct = getCheck().getConstruct();

		for(IASTDeclarator declarator : declarators) {
			construct.accept(new DeclaratorRemover(declarator));
		}
		
		String pragma = construct.toString();
		for(IASTComment comment : comments) {
			pragma += " " + comment.getRawSignature();
		}
		this.insertBefore(exparr[0], pragma + NL);
	}
	
	private int getMaxInDirection(IASTStatement statement, boolean up) {
		int i = 0;
		IASTNode next = statement;
		ReachingDefinitionsAnalysis rd = ReachingDefinitionsAnalysis.forFunction(ASTUtil.findNearestAncestor(statement, IASTFunctionDefinition.class));
		while (true) {
			next = up ? ASTUtil.getPreviousSibling(next) : ASTUtil.getNextSibling(next);
			if (next == null) {
				break;
			}
			if(containsBadControlFlowStatement(next)) {
				getCheck().getStatus().addWarning(String.format("Construct will not expand %s statement that may cause premature exit from the construct", up? "above" : "below"));
				break;
			}
			if(next instanceof IASTStatement && OpenACCUtil.isAccConstruct((IASTStatement) next)) {
				getCheck().getStatus().addInfo(String.format("Construct will not expand %s another existing OpenACC construct", up? "above" : "below"));
				break;
			}
			if(ExpandDataConstructCheck.getAccTransferProblems(rd, next, statement) != null) {
				getCheck().getStatus().addWarning(String.format("Construct will not expand %s statement that may alter values copied to or from the accelerator", up? "above" : "below"));
				break;
			}
			i++;
		}
		return i;
	}

	private boolean containsBadControlFlowStatement(IASTNode next) {
		if (!ASTUtil.find(next, IASTGotoStatement.class).isEmpty()) {
    		return true;
    	}
    	List<IASTStatement> ctlFlowStmts = new ArrayList<IASTStatement>();
    	ctlFlowStmts.addAll(ASTUtil.find(next, IASTBreakStatement.class));
    	ctlFlowStmts.addAll(ASTUtil.find(next, IASTContinueStatement.class));
    	ctlFlowStmts.addAll(ASTUtil.find(next, IASTReturnStatement.class));
    	for (IASTStatement statement : ctlFlowStmts) {
			if (!insideInner(next, statement, IASTWhileStatement.class) 
					&& !insideInner(next, statement, IASTSwitchStatement.class) 
					&& !insideInner(next, statement, IASTDoStatement.class) 
					&& !insideInner(next, statement, IASTForStatement.class)) {
				return true;
			}
    	}
    	return false;
	}

	private <T extends IASTNode> boolean insideInner(IASTNode next, IASTStatement statement, Class<T> clazz) {
		T node = ASTUtil.findNearestAncestor(statement, clazz);
    	if (node == null) {
    		return false;
    	}
    	return ASTUtil.isAncestor(node, next);
	}
	
	private IASTStatement[] getExpansionStatements(int stmtsUp, int stmtsDown, IASTStatement original) {
		List<IASTStatement> statements = new ArrayList<IASTStatement>();
		statements.add(original);

		IASTStatement current = original;
		for (int i = 0; i < stmtsUp; i++) {
			// TODO type checking
			current = (IASTStatement) ASTUtil.getPreviousSibling(current);
			statements.add(0, current);
		}

		current = original;
		for (int j = 0; j < stmtsDown; j++) {
			// TODO type checking
			current = (IASTStatement) ASTUtil.getNextSibling(current);
			statements.add(current);
		}
		return statements.toArray(new IASTStatement[statements.size()]);
	}

	protected class Expansion {

		private IASTStatement[] statements;
		private int size;

		public Expansion(IASTStatement[] statements, int size) {
			this.size = size;
			this.statements = statements;
		}

		public IASTStatement[] getStatements() {
			return statements;
		}

		public int getSize() {
			return size;
		}

	}

}
