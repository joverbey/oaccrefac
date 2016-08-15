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
package org.eclipse.ptp.pldt.openacc.internal.core.parser;

@SuppressWarnings("all")
public class CFunctionCallExpression extends ASTNode implements IAssignmentExpression, ICExpression, IConstantExpression
{
    ICExpression function; // in CFunctionCallExpression
    Token hiddenLiteralStringLparen; // in CFunctionCallExpression
    IASTListNode<IAssignmentExpression> argumentExpressionList; // in CFunctionCallExpression
    Token hiddenLiteralStringRparen; // in CFunctionCallExpression

    public ICExpression getFunction()
    {
        return this.function;
    }

    public void setFunction(ICExpression newValue)
    {
        this.function = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<IAssignmentExpression> getArgumentExpressionList()
    {
        return this.argumentExpressionList;
    }

    public void setArgumentExpressionList(IASTListNode<IAssignmentExpression> newValue)
    {
        this.argumentExpressionList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCFunctionCallExpression(this);
        visitor.visitIAssignmentExpression(this);
        visitor.visitICExpression(this);
        visitor.visitIConstantExpression(this);
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
        case 0:  return this.function;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.argumentExpressionList;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.function = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.argumentExpressionList = (IASTListNode<IAssignmentExpression>)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

