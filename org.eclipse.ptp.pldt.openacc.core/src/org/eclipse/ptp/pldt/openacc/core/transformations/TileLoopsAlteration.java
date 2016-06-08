/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

/**
 * TileLoopsAlteration defines a loop tiling refactoring algorithm. Loop tiling takes
 * a perfectly nested loop and 'tiles' the loop nest by performing loop strip mining on
 * a specified loop and afterwards interchanging the by-strip loop header as many times
 * as possible.
 * <p>
 * For example,
 * 
 * <pre>
 * for (int j = 0; j < 20; j++) {
 *     for (int i = 0; i < 10; i++) {
 *         // ...
 *     }
 * }
 * </pre>
 * 
 * Refactors to:
 * 
 * <pre>
 * for (int i_0 = 0; i_0 < 10; i_0 += 2) {
 *     for (int j = 0; j < 20; j++) {
 *         for (int i = i_0; (i < i_0 + 2 && i < 10); i++) {
 *             // ...
 *         }
 *     }
 * }
 * </pre>
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
public class TileLoopsAlteration extends AbstractTileLoopsAlteration {

    private int width;
    private int height;
    private String innerNewName;
    private String outerNewName;
    private IASTForStatement outer;
    private IASTForStatement inner;

    public TileLoopsAlteration(IASTRewrite rewriter, int width, int height, String innerNewName,
    		String outerNewName, TileLoopsCheck check) {
        super(rewriter, 0, "", check);
        this.width = width;
        this.height = height;
        this.innerNewName = innerNewName;
        this.outerNewName = outerNewName;
        this.inner = check.getInner();
        this.outer = check.getOuter();
    }

    @Override
    protected void doChange() {
        ForStatementInquisitor innerInq = ForStatementInquisitor.getInquisitor(inner);
        ForStatementInquisitor outerInq = ForStatementInquisitor.getInquisitor(outer);
        String innerIndexVar = innerInq.getIndexVariable().toString();
        String outerIndexVar = outerInq.getIndexVariable().toString();
        String innerUb = ((IASTBinaryExpression) inner.getConditionExpression()).getOperand2().getRawSignature();
        String outerUb = ((IASTBinaryExpression) outer.getConditionExpression()).getOperand2().getRawSignature();
        IASTComment[] outerComments = getBodyComments(outer);
        // inner comments will fall into the body of loop4
        if (innerNewName.equals("")) {
        	try {
        		innerNewName = createNewName(innerIndexVar, inner.getScope().getParent());
        	} catch (DOMException e) {
        		e.printStackTrace();
        		return;
        	}
        }
        if (outerNewName.equals("")) {
        	try {
        		outerNewName = createNewName(outerIndexVar, outer.getScope().getParent());
        	} catch (DOMException e) {
        		e.printStackTrace();
        		return;
        	}
        }

        // --------loop4----------------------------

        String init4, cond4, iter4, body4, loop4;
        if (inner.getInitializerStatement() instanceof IASTDeclarationStatement) {
            init4 = String.format("int %s = %s", innerIndexVar, innerNewName);
        } else {
            init4 = String.format("%s = %s", innerIndexVar, innerNewName);
        }
        cond4 = parenth(
                String.format("%s <  %s + %d && %s < %s", innerIndexVar, innerNewName, width, innerIndexVar, innerUb));
        iter4 = inner.getIterationExpression().getRawSignature();
        body4 = "";
        IASTNode[] innerBodyObjects = getBodyObjects(inner);
        for (IASTNode bodyObject : innerBodyObjects) {
            body4 += bodyObject.getRawSignature() + System.lineSeparator();
        }
        loop4 = forLoop(init4, cond4, iter4, compound(body4));

        // --------loop3----------------------------

        String init3, cond3, iter3, loop3;
        if (outer.getInitializerStatement() instanceof IASTDeclarationStatement) {
            init3 = String.format("int %s = %s", outerIndexVar, outerNewName);
        } else {
            init3 = String.format("%s = %s", outerIndexVar, outerNewName);
        }
        cond3 = parenth(
                String.format("%s <  %s + %d && %s < %s", outerIndexVar, outerNewName, height, outerIndexVar, outerUb));
        iter3 = outer.getIterationExpression().getRawSignature();
        for (int i = outerComments.length - 1; i >= 0; i--) {
            loop4 = outerComments[i].getRawSignature() + System.lineSeparator() + loop4;
        }
        loop3 = forLoop(init3, cond3, iter3, compound(loop4));

        // --------loop2----------------------------

        String innerInitRhs;
        String init2, cond2, iter2, loop2;
        // TODO we're making a lot of typecast assumptions - be sure they won't break anything
        if (inner.getInitializerStatement() instanceof IASTExpressionStatement) {
            IASTExpressionStatement es = (IASTExpressionStatement) inner.getInitializerStatement();
            IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
            innerInitRhs = e.getOperand2().getRawSignature();
        } else {
            IASTDeclarationStatement ds = (IASTDeclarationStatement) inner.getInitializerStatement();
            IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
            IASTEqualsInitializer init = (IASTEqualsInitializer) dec.getDeclarators()[0].getInitializer();
            innerInitRhs = init.getInitializerClause().getRawSignature();
        }
        init2 = String.format("int %s = %s", innerNewName, innerInitRhs);
        cond2 = String.format("%s < %s", innerNewName, innerUb);
        iter2 = String.format("%s += %d", innerNewName, width);
        loop2 = forLoop(init2, cond2, iter2, compound(loop3));

        // --------loop1----------------------------

        String outerInitRhs;
        String init1, cond1, iter1, loop1;
        // TODO we're making a lot of typecast assumptions - be sure they won't break anything
        if (outer.getInitializerStatement() instanceof IASTExpressionStatement) {
            IASTExpressionStatement es = (IASTExpressionStatement) outer.getInitializerStatement();
            IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
            outerInitRhs = e.getOperand2().getRawSignature();
        } else {
            IASTDeclarationStatement ds = (IASTDeclarationStatement) outer.getInitializerStatement();
            IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
            IASTEqualsInitializer init = (IASTEqualsInitializer) dec.getDeclarators()[0].getInitializer();
            outerInitRhs = init.getInitializerClause().getRawSignature();
        }
        init1 = String.format("int %s = %s", outerNewName, outerInitRhs);
        cond1 = String.format("%s < %s", outerNewName, outerUb);
        iter1 = String.format("%s += %d", outerNewName, height);
        loop1 = forLoop(init1, cond1, iter1, compound(loop2));

        // ------Done------------------------

        this.replace(outer, loop1);
        finalizeChanges();

    }

	@Override
	protected String getOuterCond(String newName, String compOp, String ub, int numValue) {
		return null;
	}

	@Override
	protected String getOuterIter(String newName, int numFactor) {
		return null;
	}

	@Override
	protected String getInnerCond(String indexVar, String newName, int numFactor, String compOp, String ub) {
		return null;
	}

	@Override
	protected String getInnerIter(IASTForStatement loop, String indexVar, String ub, int NumValue) {
		return null;
	}

}