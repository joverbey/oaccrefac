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
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyinClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyoutClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCreateClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataItemNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTVisitor;
import org.eclipse.ptp.pldt.openacc.core.parser.IASTListNode;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class Promote extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

	IASTForStatement container;
	
	public Promote(IASTRewrite rewriter, ExpandDataConstructCheck check, IASTForStatement container) {
		super(rewriter, check);
		this.container = container;
	}
	
	@Override
	public void doChange() {
		replace(getPragma(), getStatement().getRawSignature());
		remove(container.getBody());
		
		List<IASTComment> commentsWithPragma = new ArrayList<IASTComment>();
		for(IASTComment comment : getStatement().getTranslationUnit().getComments()) {
			if(comment.getFileLocation().getStartingLineNumber() == getPragma().getFileLocation().getStartingLineNumber()) {
				commentsWithPragma.add(comment);
				this.remove(comment);
			}
		}
		
		String pragma = buildNewPragma(new IASTNode[] { container.getInitializerStatement(), container.getConditionExpression(),
				container.getIterationExpression() }, commentsWithPragma);
		insertBefore(container, pragma + NL + LCURLY);
		insertAfter(container, RCURLY);
		finalizeChanges();
	}
	
	private String buildNewPragma(IASTNode[] exparr, List<IASTComment> comments) {

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
		for (IASTNode stmt : exparr) {
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
		
		return pragma;
	}
	
}
