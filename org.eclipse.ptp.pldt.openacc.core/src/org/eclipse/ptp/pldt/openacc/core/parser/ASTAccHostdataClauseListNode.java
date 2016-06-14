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
public class ASTAccHostdataClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccHostdataClauseListNode
    IAccHostdataClause accHostdataClause; // in ASTAccHostdataClauseListNode

    public IAccHostdataClause getAccHostdataClause()
    {
        return this.accHostdataClause;
    }

    public void setAccHostdataClause(IAccHostdataClause newValue)
    {
        this.accHostdataClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccHostdataClauseListNode(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 2;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.hiddenLiteralStringComma;
        case 1:  return this.accHostdataClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accHostdataClause = (IAccHostdataClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

