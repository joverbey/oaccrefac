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
public class ASTAccEnterDataNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccEnterDataNode
    Token hiddenLiteralStringEnter; // in ASTAccEnterDataNode
    Token hiddenLiteralStringData; // in ASTAccEnterDataNode
    IASTListNode<ASTAccEnterDataClauseListNode> accEnterDataClauseList; // in ASTAccEnterDataNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccEnterDataClauseListNode> getAccEnterDataClauseList()
    {
        return this.accEnterDataClauseList;
    }

    public void setAccEnterDataClauseList(IASTListNode<ASTAccEnterDataClauseListNode> newValue)
    {
        this.accEnterDataClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccEnterDataNode(this);
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
        case 1:  return this.hiddenLiteralStringEnter;
        case 2:  return this.hiddenLiteralStringData;
        case 3:  return this.accEnterDataClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringEnter = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringData = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.accEnterDataClauseList = (IASTListNode<ASTAccEnterDataClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

