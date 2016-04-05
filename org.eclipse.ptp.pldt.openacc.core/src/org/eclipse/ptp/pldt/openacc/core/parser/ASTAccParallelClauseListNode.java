package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccParallelClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccParallelClauseListNode
    IAccParallelClause accParallelClause; // in ASTAccParallelClauseListNode

    public IAccParallelClause getAccParallelClause()
    {
        return this.accParallelClause;
    }

    public void setAccParallelClause(IAccParallelClause newValue)
    {
        this.accParallelClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccParallelClauseListNode(this);
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
        case 1:  return this.accParallelClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accParallelClause = (IAccParallelClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

