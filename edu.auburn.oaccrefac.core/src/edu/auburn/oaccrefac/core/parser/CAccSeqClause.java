package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CAccSeqClause extends ASTNode implements IAccKernelsLoopClause, IAccLoopClause, IAccParallelLoopClause, IAccRoutineClause
{
    Token hiddenLiteralStringSeq; // in CAccSeqClause

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCAccSeqClause(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccLoopClause(this);
        visitor.visitIAccParallelLoopClause(this);
        visitor.visitIAccRoutineClause(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 1;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.hiddenLiteralStringSeq;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringSeq = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

