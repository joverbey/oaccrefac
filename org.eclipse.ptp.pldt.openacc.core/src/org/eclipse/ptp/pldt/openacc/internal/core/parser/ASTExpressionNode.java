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
public class ASTExpressionNode extends ASTNode implements ICExpression
{
    private IASTListNode<Token> prependedTokens = new ASTListNode<Token>();
    private IASTListNode<Token> appendedTokens = new ASTListNode<Token>();
    ICExpression conditionalExpression; // in ASTExpressionNode

    public ICExpression getConditionalExpression()
    {
        return this.conditionalExpression;
    }

    public void setConditionalExpression(ICExpression newValue)
    {
        this.conditionalExpression = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTExpressionNode(this);
        visitor.visitICExpression(this);
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
        case 0:  return this.prependedTokens;
        case 1:  return this.conditionalExpression;
        case 2:  return this.appendedTokens;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.prependedTokens = (IASTListNode<Token>)value; if (value != null) value.setParent(this); return;
        case 1:  this.conditionalExpression = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 2:  this.appendedTokens = (IASTListNode<Token>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    IASTListNode<Token> getPrependedTokens()
    {
        return prependedTokens;
    }

    IASTListNode<Token> getAppendedTokens()
    {
        return appendedTokens;
    }

    public void prependToken(Token token)
    {
        prependedTokens.add(0, token);
        token.setParent(this);
    }

    public void appendToken(Token token)
    {
        appendedTokens.add(token);
        token.setParent(this);
    }
}

