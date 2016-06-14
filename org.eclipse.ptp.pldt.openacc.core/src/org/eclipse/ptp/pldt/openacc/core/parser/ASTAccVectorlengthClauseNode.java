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
public class ASTAccVectorlengthClauseNode extends ASTNode implements IAccParallelClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringVectorUnderscorelength; // in ASTAccVectorlengthClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccVectorlengthClauseNode
    IConstantExpression count; // in ASTAccVectorlengthClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccVectorlengthClauseNode

    public IConstantExpression getCount()
    {
        return this.count;
    }

    public void setCount(IConstantExpression newValue)
    {
        this.count = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccVectorlengthClauseNode(this);
        visitor.visitIAccParallelClause(this);
        visitor.visitIAccParallelLoopClause(this);
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
        case 0:  return this.hiddenLiteralStringVectorUnderscorelength;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.count;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringVectorUnderscorelength = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.count = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

