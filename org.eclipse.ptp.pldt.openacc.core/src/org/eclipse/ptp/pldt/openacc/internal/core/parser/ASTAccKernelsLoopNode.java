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
public class ASTAccKernelsLoopNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccKernelsLoopNode
    Token hiddenLiteralStringKernels; // in ASTAccKernelsLoopNode
    Token hiddenLiteralStringLoop; // in ASTAccKernelsLoopNode
    IASTListNode<ASTAccKernelsLoopClauseListNode> accKernelsLoopClauseList; // in ASTAccKernelsLoopNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccKernelsLoopClauseListNode> getAccKernelsLoopClauseList()
    {
        return this.accKernelsLoopClauseList;
    }

    public void setAccKernelsLoopClauseList(IASTListNode<ASTAccKernelsLoopClauseListNode> newValue)
    {
        this.accKernelsLoopClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccKernelsLoopNode(this);
        visitor.visitIAccConstruct(this);
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
        case 0:  return this.pragmaAcc;
        case 1:  return this.hiddenLiteralStringKernels;
        case 2:  return this.hiddenLiteralStringLoop;
        case 3:  return this.accKernelsLoopClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringKernels = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringLoop = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.accKernelsLoopClauseList = (IASTListNode<ASTAccKernelsLoopClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

