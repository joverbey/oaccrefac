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
public class ASTAccDeviceptrClauseNode extends ASTNode implements IAccDataClause, IAccDeclareClause, IAccKernelsClause, IAccKernelsLoopClause, IAccParallelClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringDeviceptr; // in ASTAccDeviceptrClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccDeviceptrClauseNode
    IASTListNode<ASTIdentifierNode> identifierList; // in ASTAccDeviceptrClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccDeviceptrClauseNode

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
        visitor.visitASTAccDeviceptrClauseNode(this);
        visitor.visitIAccDataClause(this);
        visitor.visitIAccDeclareClause(this);
        visitor.visitIAccKernelsClause(this);
        visitor.visitIAccKernelsLoopClause(this);
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
        case 0:  return this.hiddenLiteralStringDeviceptr;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.identifierList;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringDeviceptr = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.identifierList = (IASTListNode<ASTIdentifierNode>)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

