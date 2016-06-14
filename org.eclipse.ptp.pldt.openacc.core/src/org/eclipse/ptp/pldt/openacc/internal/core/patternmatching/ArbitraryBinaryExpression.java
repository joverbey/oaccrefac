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
package org.eclipse.ptp.pldt.openacc.internal.core.patternmatching;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;

public class ArbitraryBinaryExpression extends ArbitraryExpression implements IASTBinaryExpression {
    
    private IASTExpression lValue;
    private IASTExpression rValue;
    private int opcode;
    
    public ArbitraryBinaryExpression(IASTExpression lValue, int opcode, IASTExpression rValue) {
        this.lValue = lValue;
        this.opcode = opcode;
        this.rValue = rValue;
    }

    @Override
    public void setOperator(int op) {
        opcode = op;
    }

    @Override
    public int getOperator() {
        return opcode;
    }

    @Override
    public IASTExpression getOperand1() {
        return lValue;
    }

    @Override
    public void setOperand1(IASTExpression expression) {
        lValue = expression;
    }

    @Override
    public IASTExpression getOperand2() {
        return rValue;
    }

    @Override
    public IASTInitializerClause getInitOperand2() {
        return null;
    }

    @Override
    public void setOperand2(IASTExpression expression) {
        rValue = expression;
    }

    @Override
    public ArbitraryBinaryExpression copy() {
        return new ArbitraryBinaryExpression(lValue.copy(), opcode, rValue.copy());
    }

    @Override
    public ArbitraryBinaryExpression copy(CopyStyle style) {
        return new ArbitraryBinaryExpression(lValue.copy(), opcode, rValue.copy());
    }

}
