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
public class CBinaryExpression extends ASTNode implements IAssignmentExpression, ICExpression, IConstantExpression
{
    ICExpression lhs; // in CBinaryExpression
    Token operator; // in CBinaryExpression
    ICExpression rhs; // in CBinaryExpression

    public ICExpression getLhs()
    {
        return this.lhs;
    }

    public void setLhs(ICExpression newValue)
    {
        this.lhs = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public Token getOperator()
    {
        return this.operator;
    }

    public void setOperator(Token newValue)
    {
        this.operator = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ICExpression getRhs()
    {
        return this.rhs;
    }

    public void setRhs(ICExpression newValue)
    {
        this.rhs = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCBinaryExpression(this);
        visitor.visitIAssignmentExpression(this);
        visitor.visitICExpression(this);
        visitor.visitIConstantExpression(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 3;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.lhs;
        case 1:  return this.operator;
        case 2:  return this.rhs;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.lhs = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 1:  this.operator = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.rhs = (ICExpression)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

