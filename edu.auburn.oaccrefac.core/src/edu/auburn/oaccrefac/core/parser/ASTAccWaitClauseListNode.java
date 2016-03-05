package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccWaitClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccWaitClauseListNode
    ASTAccAsyncClauseNode accAsyncClause; // in ASTAccWaitClauseListNode

    public ASTAccAsyncClauseNode getAccAsyncClause()
    {
        return this.accAsyncClause;
    }

    public void setAccAsyncClause(ASTAccAsyncClauseNode newValue)
    {
        this.accAsyncClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccWaitClauseListNode(this);
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
        case 1:  return this.accAsyncClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accAsyncClause = (ASTAccAsyncClauseNode)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

