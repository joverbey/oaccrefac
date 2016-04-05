package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccDataClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccDataClauseListNode
    IAccDataClause accDataClause; // in ASTAccDataClauseListNode

    public IAccDataClause getAccDataClause()
    {
        return this.accDataClause;
    }

    public void setAccDataClause(IAccDataClause newValue)
    {
        this.accDataClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccDataClauseListNode(this);
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
        case 1:  return this.accDataClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accDataClause = (IAccDataClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

