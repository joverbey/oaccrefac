package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccDeclareClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccDeclareClauseListNode
    IAccDeclareClause accDeclareClause; // in ASTAccDeclareClauseListNode

    public IAccDeclareClause getAccDeclareClause()
    {
        return this.accDeclareClause;
    }

    public void setAccDeclareClause(IAccDeclareClause newValue)
    {
        this.accDeclareClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccDeclareClauseListNode(this);
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
        case 1:  return this.accDeclareClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accDeclareClause = (IAccDeclareClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

