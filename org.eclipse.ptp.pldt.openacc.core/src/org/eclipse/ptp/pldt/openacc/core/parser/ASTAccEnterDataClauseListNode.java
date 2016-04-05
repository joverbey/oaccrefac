package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccEnterDataClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccEnterDataClauseListNode
    IAccEnterDataClause accEnterDataClause; // in ASTAccEnterDataClauseListNode

    public IAccEnterDataClause getAccEnterDataClause()
    {
        return this.accEnterDataClause;
    }

    public void setAccEnterDataClause(IAccEnterDataClause newValue)
    {
        this.accEnterDataClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccEnterDataClauseListNode(this);
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
        case 1:  return this.accEnterDataClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accEnterDataClause = (IAccEnterDataClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

