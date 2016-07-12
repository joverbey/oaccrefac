/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
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
    private IASTForStatement inner;
    private IASTForStatement outer;

    public TileLoopsAlteration(IASTRewrite rewriter, int width, int height, String innerNewName,
    		String outerNewName, TileLoopsCheck check) {
        super(rewriter, check);
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
        IASTBinaryExpression innerConditionExpression = (IASTBinaryExpression) inner.getConditionExpression();
        IASTBinaryExpression outerConditionExpression = (IASTBinaryExpression) outer.getConditionExpression();
		String innerUb = innerConditionExpression.getOperand2().getRawSignature();
		String outerUb = outerConditionExpression.getOperand2().getRawSignature();
		String innerComparisonOperator = getOperatorAsString(innerConditionExpression);
		String outerComparisonOperator = getOperatorAsString(outerConditionExpression);
        if (innerNewName.equals("")) { //$NON-NLS-1$
        	try {
        		innerNewName = createNewName(innerIndexVar, inner.getScope().getParent());
        	} catch (DOMException e) {
        		e.printStackTrace();
        		return;
        	}
        }
        if (outerNewName.equals("")) { //$NON-NLS-1$
        	try {
        		outerNewName = createNewName(outerIndexVar, outer.getScope().getParent());
        	} catch (DOMException e) {
        		e.printStackTrace();
        		return;
        	}
        }

        // --------loop4----------------------------

        String init4, cond4, iter4, body4, loop4, innerType;
        if (inner.getInitializerStatement() instanceof IASTDeclarationStatement) {
        	IASTDeclarationStatement ds = (IASTDeclarationStatement) inner.getInitializerStatement();
            IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
            innerType = dec.getDeclSpecifier().getRawSignature();
            init4 = String.format("%s %s = %s", innerType, innerIndexVar, innerNewName); //$NON-NLS-1$
        } else {
        	IASTExpressionStatement es = (IASTExpressionStatement) inner.getInitializerStatement();
            IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
        	IType type = e.getOperand1().getExpressionType();
            if (type instanceof ITypedef) {
            	innerType = ((ITypedef) type).getType().toString();
            }
            else {
            	innerType = type.toString();
            }
            init4 = String.format("%s = %s", innerIndexVar, innerNewName); //$NON-NLS-1$
        }
        cond4 = parenth(
                String.format("%s <  %s + %d && %s %s %s", innerIndexVar, innerNewName, width, innerIndexVar,  //$NON-NLS-1$
                		innerComparisonOperator, innerUb));
        iter4 = inner.getIterationExpression().getRawSignature();
        body4 = inner.getBody().getRawSignature();
        loop4 = forLoop(init4, cond4, iter4, body4);

        // --------loop3----------------------------

        String init3, cond3, iter3, loop3, outerType;
        if (outer.getInitializerStatement() instanceof IASTDeclarationStatement) {
        	IASTDeclarationStatement ds = (IASTDeclarationStatement) outer.getInitializerStatement();
            IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
            outerType = dec.getDeclSpecifier().getRawSignature();
            init3 = String.format("%s %s = %s", outerType, outerIndexVar, outerNewName); //$NON-NLS-1$
        } else {
        	IASTExpressionStatement es = (IASTExpressionStatement) outer.getInitializerStatement();
            IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
        	IType type = e.getOperand1().getExpressionType();
            if (type instanceof ITypedef) {
            	outerType = ((ITypedef) type).getType().toString();
            }
            else {
            	outerType = type.toString();
            }
            init3 = String.format("%s = %s", outerIndexVar, outerNewName); //$NON-NLS-1$
        }
        cond3 = parenth(
                String.format("%s <  %s + %d && %s %s %s", outerIndexVar, outerNewName, height, outerIndexVar,  //$NON-NLS-1$
                		outerComparisonOperator, outerUb));
        iter3 = outer.getIterationExpression().getRawSignature();
        IASTComment[] outerComments = getBodyComments(outer);
        for (IASTComment comment : outerComments) {
        	loop4 = comment.getRawSignature() + loop4;
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
        init2 = String.format("%s %s = %s", innerType, innerNewName, innerInitRhs); //$NON-NLS-1$
        cond2 = String.format("%s %s %s", innerNewName, innerComparisonOperator, innerUb); //$NON-NLS-1$
        iter2 = String.format("%s += %d", innerNewName, width); //$NON-NLS-1$
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
        init1 = String.format("%s %s = %s", outerType, outerNewName, outerInitRhs); //$NON-NLS-1$
        cond1 = String.format("%s %s %s", outerNewName, outerComparisonOperator, outerUb); //$NON-NLS-1$
        iter1 = String.format("%s += %d", outerNewName, height); //$NON-NLS-1$
        loop1 = forLoop(init1, cond1, iter1, compound(loop2));

        // ------Done------------------------

        this.replace(outer, loop1);
        finalizeChanges();

    }

}