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

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyinClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyoutClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCreateClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataItemNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTVisitor;
import org.eclipse.ptp.pldt.openacc.core.parser.IASTListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;
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
	
	private void insertNewPragma(IASTStatement[] exparr,List<IASTComment> comments) {

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
		
		IAccConstruct construct;
		try {
			construct = new OpenACCParser().parse(getPragma().getRawSignature());
		} catch (Exception e) {
			System.err.println("Failed to parse data construct");
			e.printStackTrace();
			String pragma = getPragma().getRawSignature();
			for(IASTComment comment : comments) {
				pragma += " " + comment.getRawSignature();
			}
			this.insertBefore(exparr[0], pragma + NL);
			return;
		}

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
		ReachingDefinitions rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(statement, IASTFunctionDefinition.class));
		while (true) {
			next = up ? ASTUtil.getPreviousSibling(next) : ASTUtil.getNextSibling(next);
			if (next == null || (next instanceof IASTStatement && OpenACCUtil.isAccConstruct((IASTStatement) next))) {
				break;
			}
			if(!ExpandDataConstructCheck.checkCopyinCopyoutReachingDefinitions(rd, next, statement)) {
				break;
			}
			i++;
		}
		return i;
	}

	/*
	 * if a def in A reaches C and is in the copyin set, dont expand
	 * if a def in A doesnt reach C and is in the copyin set, can expand
	 *     - since we're using the inferred sets, this can never happen
	 * so technically we could just check if a def in A is in the copyin set, but
	 *     this way is more correct in a sense, and there shouldn't be a big
	 *     performance hit or anything, since rd would be done anyway for inference
	 */
	
	
	
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
