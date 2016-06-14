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
public class ASTAccReductionClauseNode extends ASTNode implements IAccKernelsLoopClause, IAccLoopClause, IAccParallelClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringReduction; // in ASTAccReductionClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccReductionClauseNode
    Token operator; // in ASTAccReductionClauseNode
    Token hiddenLiteralStringColon; // in ASTAccReductionClauseNode
    IASTListNode<ASTIdentifierNode> identifierList; // in ASTAccReductionClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccReductionClauseNode

    public Token getOperator()
    {
        return this.operator;
    }

    public void setOperator(Token newValue)
    {
        this.operator = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTIdentifierNode> getIdentifierList()
    {
        return this.identifierList;
    }

    public void setIdentifierList(IASTListNode<ASTIdentifierNode> newValue)
    {
        this.identifierList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccReductionClauseNode(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccLoopClause(this);
        visitor.visitIAccParallelClause(this);
        visitor.visitIAccParallelLoopClause(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 6;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.hiddenLiteralStringReduction;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.operator;
        case 3:  return this.hiddenLiteralStringColon;
        case 4:  return this.identifierList;
        case 5:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringReduction = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.operator = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringColon = (Token)value; if (value != null) value.setParent(this); return;
        case 4:  this.identifierList = (IASTListNode<ASTIdentifierNode>)value; if (value != null) value.setParent(this); return;
        case 5:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

