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
public class ASTAccDefaultnoneClauseNode extends ASTNode implements IAccKernelsClause, IAccKernelsLoopClause, IAccParallelClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringDefault; // in ASTAccDefaultnoneClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccDefaultnoneClauseNode
    Token hiddenLiteralStringNone; // in ASTAccDefaultnoneClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccDefaultnoneClauseNode

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccDefaultnoneClauseNode(this);
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
        case 0:  return this.hiddenLiteralStringDefault;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.hiddenLiteralStringNone;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringDefault = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringNone = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

