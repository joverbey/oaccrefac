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
public class ASTAccKernelsNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccKernelsNode
    Token hiddenLiteralStringKernels; // in ASTAccKernelsNode
    IASTListNode<ASTAccKernelsClauseListNode> accKernelsClauseList; // in ASTAccKernelsNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccKernelsClauseListNode> getAccKernelsClauseList()
    {
        return this.accKernelsClauseList;
    }

    public void setAccKernelsClauseList(IASTListNode<ASTAccKernelsClauseListNode> newValue)
    {
        this.accKernelsClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccKernelsNode(this);
        visitor.visitIAccConstruct(this);
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
        case 0:  return this.pragmaAcc;
        case 1:  return this.hiddenLiteralStringKernels;
        case 2:  return this.accKernelsClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringKernels = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.accKernelsClauseList = (IASTListNode<ASTAccKernelsClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

