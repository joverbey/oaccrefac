/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

/**
 * 
 * for(k = 0; k < l; k++)
 * 
 * for(int i = 0; i < l; i += S)
 *   for(int j = 0; j < S; j++)
 *     k = i + j;
 *
 */

public class ZeroBasedStripMine extends ForLoopAlteration<StripMineCheck> {

	private int stripFactor;
	private boolean handleOverflow;
	private String newNameOuter;
	private String newNameInner;
	
	public ZeroBasedStripMine(IASTRewrite rewriter, int stripFactor, boolean handleOverflow, String newNameOuter, String newNameInner, StripMineCheck check) {
		super(rewriter, check);
		this.stripFactor = stripFactor;
		this.handleOverflow = handleOverflow;
		this.newNameOuter = newNameOuter;
		this.newNameInner = newNameInner;
	}
	
	@Override
	protected void doChange() {
		IASTForStatement loop = getLoop();
		ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(loop);
		String indexVar = inq.getIndexVariable().toString();
		try {
			if (newNameOuter.equals("")) { //$NON-NLS-1$
				newNameOuter = createNewName(indexVar, loop.getScope().getParent());
			}
			if (newNameInner.equals("")) { //$NON-NLS-1$
				newNameInner = createNewName(indexVar, loop.getScope().getParent(), newNameOuter);
			}
		} catch (DOMException e) {
			e.printStackTrace();
			return;
		}
		
		String initType, indexVarStmt;
		String innerInit, innerCond, innerIter, innerBody, inner;
		String outerInit, outerCond, outerIter, outer;
		IASTBinaryExpression condExpr = (IASTBinaryExpression) loop.getConditionExpression();
		String compOp = getOperatorAsString(condExpr);
		String ub = condExpr.getOperand2().getRawSignature();
		
		if (loop.getInitializerStatement() instanceof IASTExpressionStatement) {
			IASTExpressionStatement es = (IASTExpressionStatement) loop.getInitializerStatement();
			IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
			IType type = e.getOperand1().getExpressionType();
			if (type instanceof ITypedef) {
				initType = ((ITypedef) type).getType().toString();
			} else {
				initType = type.toString();
			}
		} else {
			IASTDeclarationStatement ds = (IASTDeclarationStatement) loop.getInitializerStatement();
			IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
			initType = dec.getDeclSpecifier().getRawSignature();
		}
		
		innerInit = String.format("%s %s = 0", initType, newNameInner); //$NON-NLS-1$
		if(handleOverflow) {
			innerCond = String.format("%s < %d && %s + %s %s %s", newNameInner, stripFactor, newNameOuter, newNameInner, compOp, ub); //$NON-NLS-1$
		}
		else {
			innerCond = String.format("%s < %d", newNameInner, stripFactor); //$NON-NLS-1$
		}
		if(inq.getIterationFactor() == 1) {
			innerIter = String.format("%s++", newNameInner); //$NON-NLS-1$
		}
		else {
			innerIter = String.format("%s += %d", newNameInner, inq.getIterationFactor()); //$NON-NLS-1$
		}
		
		outerInit = String.format("%s %s = 0", initType, newNameOuter); //$NON-NLS-1$
		outerCond = String.format("%s < %s", newNameOuter, ub); //$NON-NLS-1$
		outerIter = String.format("%s += %d", newNameOuter, stripFactor); //$NON-NLS-1$
		
		innerBody = decompound(loop.getBody().getRawSignature());
		if (!(loop.getBody() instanceof IASTCompoundStatement)) {
			for (IASTComment comment : getBodyComments(loop)) {
				innerBody = comment.getRawSignature() + NL + innerBody;
			}
		}
		if (loop.getInitializerStatement() instanceof IASTDeclarationStatement) {
			indexVarStmt = String.format("%s %s = %s + %s;", initType, indexVar, newNameOuter, newNameInner); //$NON-NLS-1$
		} else {
			indexVarStmt = String.format("%s = %s + %s;", indexVar, newNameOuter, newNameInner); //$NON-NLS-1$
		}
		innerBody = indexVarStmt + NL + innerBody;
		inner = forLoop(innerInit, innerCond, innerIter, compound(innerBody));
		outer = forLoop(outerInit, outerCond, outerIter, compound(inner));
		
		this.replace(loop, outer);
		
		finalizeChanges();
	}
	
	protected String getOperatorAsString(IASTBinaryExpression condExpr) {
		String compOp;
		switch (condExpr.getOperator()) {
		case IASTBinaryExpression.op_lessEqual:
			compOp = "<="; //$NON-NLS-1$
			break;
		case IASTBinaryExpression.op_lessThan:
			compOp = "<"; //$NON-NLS-1$
			break;
		default:
			throw new IllegalStateException();
		}
		return compOp;
	}

}
