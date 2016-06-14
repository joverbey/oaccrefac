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
package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class CArrayAccessExpression extends ASTNode implements ICExpression
{
    ICExpression array; // in CArrayAccessExpression
    Token hiddenLiteralStringLbracket; // in CArrayAccessExpression
    ASTExpressionNode subscript; // in CArrayAccessExpression
    Token hiddenLiteralStringRbracket; // in CArrayAccessExpression

    public ICExpression getArray()
    {
        return this.array;
    }

    public void setArray(ICExpression newValue)
    {
        this.array = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ASTExpressionNode getSubscript()
    {
        return this.subscript;
    }

    public void setSubscript(ASTExpressionNode newValue)
    {
        this.subscript = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCArrayAccessExpression(this);
        visitor.visitICExpression(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 4;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.array;
        case 1:  return this.hiddenLiteralStringLbracket;
        case 2:  return this.subscript;
        case 3:  return this.hiddenLiteralStringRbracket;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.array = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLbracket = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.subscript = (ASTExpressionNode)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRbracket = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

