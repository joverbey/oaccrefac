/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

/**
 * StripMineAlteration defines a loop strip mine refactoring algorithm. Loop strip
 * mining takes a sequential loop and essentially creates 'strips' through perfectly
 * nesting a by-strip loop and an in-strip loop.
 * <p>
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 * 	// ...
 * }
 * </pre>
 * 
 * Refactors to: Refactors to: The outer loop is the by-strip and the inner loop is the
 * in-strip loop...
 * 
 * <pre>
 * for (int i_0 = 0; i_0 < 10; i_0 += 2) {
 * 	for (int i = i_0; (i < i_0 + 2 && i < 10); i++) {
 * 		// ...
 * 	}
 * }
 * </pre>
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
public class StripMineAlteration extends ForLoopAlteration<StripMineCheck> {

	private int stripFactor;
	private boolean zeroBased;
	private boolean handleOverflow;
	private String newNameOuter;
	private String newNameInner;

	/**
	 * Constructor. Takes parameters for strip factor and strip depth to tell the refactoring which perfectly nested
	 * loop to strip mine.
	 * 
	 * @author Adam Eichelkraut
	 * @param rewriter
	 *            -- rewriter associated with the for loop
	 * @param stripFactor
	 *            -- factor for how large strips are
	 */
	public StripMineAlteration(IASTRewrite rewriter, int stripFactor, boolean zeroBased, boolean handleOverflow,
			String newNameOuter, String newNameInner, StripMineCheck check) {
		super(rewriter, check);
		this.stripFactor = stripFactor;
		this.zeroBased = zeroBased;
		this.handleOverflow = handleOverflow;
		this.newNameOuter = newNameOuter;
		this.newNameInner = newNameInner;
	}

	@Override
	protected void doChange() {
		IASTForStatement loop = getLoop();
		ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(loop);
		String indexVar = inq.getIndexVariable().toString();
		if (newNameOuter.equals("")) {
			try {
				newNameOuter = createNewName(indexVar, loop.getScope().getParent());
			} catch (DOMException e) {
				e.printStackTrace();
				return;
			}
		}

		String innerInit, innerCond, innerIter, innerBody, inner;
		String outerInit, outerCond, outerIter, outer;
		IASTBinaryExpression condExpr = (IASTBinaryExpression) loop.getConditionExpression();
		String compOp = getOperatorAsString(condExpr);
		String ub = condExpr.getOperand2().getRawSignature();
		if (loop.getInitializerStatement() instanceof IASTDeclarationStatement) {
			innerInit = String.format("int %s = %s", indexVar, newNameOuter);
		} else {
			innerInit = String.format("%s = %s", indexVar, newNameOuter);
		}
		innerCond = getInnerCond(indexVar, newNameOuter, stripFactor, compOp, ub);
		innerIter = getInnerIter(loop, indexVar, ub, stripFactor);
		innerBody = loop.getBody().getRawSignature();
		if (!(loop.getBody() instanceof IASTCompoundStatement)) {
			IASTComment[] outerComments = getBodyComments(loop);
			for (IASTComment comment : outerComments) {
				innerBody = comment.getRawSignature() + "\n" + innerBody;
			}
			innerBody = compound(innerBody);
		}
		inner = forLoop(innerInit, innerCond, innerIter, innerBody);

		String initRhs;
		String initType;
		// TODO we're making a lot of typecast assumptions - be sure they won't break anything
		if (loop.getInitializerStatement() instanceof IASTExpressionStatement) {
			IASTExpressionStatement es = (IASTExpressionStatement) loop.getInitializerStatement();
			IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
			initRhs = e.getOperand2().getRawSignature();
			IType type = e.getOperand1().getExpressionType();
			if (type instanceof ITypedef) {
				initType = ((ITypedef) type).getType().toString();
			} else {
				initType = type.toString();
			}
		} else {
			IASTDeclarationStatement ds = (IASTDeclarationStatement) loop.getInitializerStatement();
			IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
			IASTEqualsInitializer init = (IASTEqualsInitializer) dec.getDeclarators()[0].getInitializer();
			initRhs = init.getInitializerClause().getRawSignature();
			initType = dec.getDeclSpecifier().getRawSignature();
		}
		outerInit = String.format("%s %s = %s", initType, newNameOuter, initRhs);
		outerCond = getOuterCond(newNameOuter, compOp, ub, stripFactor);
		outerIter = getOuterIter(newNameOuter, stripFactor);
		outer = forLoop(outerInit, outerCond, outerIter, compound(inner));
		this.replace(loop, outer);
		finalizeChanges();
	}

	protected String getOperatorAsString(IASTBinaryExpression condExpr) {
		String compOp;
		switch (condExpr.getOperator()) {
		case IASTBinaryExpression.op_lessEqual:
			compOp = "<=";
			break;
		case IASTBinaryExpression.op_lessThan:
			compOp = "<";
			break;
		default:
			throw new IllegalStateException();
		}
		return compOp;
	}

	protected String getOuterCond(String newName, String compOp, String ub, int numValue) {
		return String.format("%s %s %s", newName, compOp, ub);
	}

	protected String getOuterIter(String newName, int numFactor) {
		return String.format("%s += %d", newName, numFactor);
	}

	protected String getInnerCond(String indexVar, String newName, int numFactor,
			String compOp, String ub) {
		return parenth(String.format("%s <  %s + %d && %s %s %s", indexVar, newName, numFactor, indexVar, compOp, ub));
	}

	protected String getInnerIter(IASTForStatement loop, String indexVar, String ub, int numValue) {
		return loop.getIterationExpression().getRawSignature();
	}

}
