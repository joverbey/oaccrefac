package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccLoopClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccLoopClauseListNode
    IAccLoopClause accLoopClause; // in ASTAccLoopClauseListNode

    public IAccLoopClause getAccLoopClause()
    {
        return this.accLoopClause;
    }

    public void setAccLoopClause(IAccLoopClause newValue)
    {
        this.accLoopClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccLoopClauseListNode(this);
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
        case 1:  return this.accLoopClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accLoopClause = (IAccLoopClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

