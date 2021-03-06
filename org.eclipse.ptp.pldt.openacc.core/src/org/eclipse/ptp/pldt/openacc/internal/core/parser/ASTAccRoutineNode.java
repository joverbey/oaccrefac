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
public class ASTAccRoutineNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccRoutineNode
    Token hiddenLiteralStringRoutine; // in ASTAccRoutineNode
    Token hiddenLiteralStringLparen; // in ASTAccRoutineNode
    ASTIdentifierNode name; // in ASTAccRoutineNode
    Token hiddenLiteralStringRparen; // in ASTAccRoutineNode
    IASTListNode<ASTAccRoutineClauseListNode> accRoutineClauseList; // in ASTAccRoutineNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ASTIdentifierNode getName()
    {
        return this.name;
    }

    public void setName(ASTIdentifierNode newValue)
    {
        this.name = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccRoutineClauseListNode> getAccRoutineClauseList()
    {
        return this.accRoutineClauseList;
    }

    public void setAccRoutineClauseList(IASTListNode<ASTAccRoutineClauseListNode> newValue)
    {
        this.accRoutineClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccRoutineNode(this);
        visitor.visitIAccConstruct(this);
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
        case 0:  return this.pragmaAcc;
        case 1:  return this.hiddenLiteralStringRoutine;
        case 2:  return this.hiddenLiteralStringLparen;
        case 3:  return this.name;
        case 4:  return this.hiddenLiteralStringRparen;
        case 5:  return this.accRoutineClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringRoutine = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.name = (ASTIdentifierNode)value; if (value != null) value.setParent(this); return;
        case 4:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        case 5:  this.accRoutineClauseList = (IASTListNode<ASTAccRoutineClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

