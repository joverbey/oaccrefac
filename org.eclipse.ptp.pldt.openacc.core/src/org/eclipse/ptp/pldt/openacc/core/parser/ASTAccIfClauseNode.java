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
public class ASTAccIfClauseNode extends ASTNode implements IAccDataClause, IAccEnterDataClause, IAccExitDataClause, IAccKernelsClause, IAccKernelsLoopClause, IAccParallelClause, IAccParallelLoopClause, IAccUpdateClause
{
    Token hiddenLiteralStringIf; // in ASTAccIfClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccIfClauseNode
    ICExpression conditionalExpression; // in ASTAccIfClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccIfClauseNode

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
        visitor.visitASTAccIfClauseNode(this);
        visitor.visitIAccDataClause(this);
        visitor.visitIAccEnterDataClause(this);
        visitor.visitIAccExitDataClause(this);
        visitor.visitIAccKernelsClause(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccParallelClause(this);
        visitor.visitIAccParallelLoopClause(this);
        visitor.visitIAccUpdateClause(this);
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
        case 0:  return this.hiddenLiteralStringIf;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.conditionalExpression;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringIf = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.conditionalExpression = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

