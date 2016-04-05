package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccParallelLoopClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccParallelLoopClauseListNode
    IAccParallelLoopClause accParallelLoopClause; // in ASTAccParallelLoopClauseListNode

    public IAccParallelLoopClause getAccParallelLoopClause()
    {
        return this.accParallelLoopClause;
    }

    public void setAccParallelLoopClause(IAccParallelLoopClause newValue)
    {
        this.accParallelLoopClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccParallelLoopClauseListNode(this);
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
        case 1:  return this.accParallelLoopClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accParallelLoopClause = (IAccParallelLoopClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

