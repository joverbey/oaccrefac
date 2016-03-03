package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CAccIndependentClause extends ASTNode implements IAccKernelsLoopClause, IAccLoopClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringIndependent; // in CAccIndependentClause

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCAccIndependentClause(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccLoopClause(this);
        visitor.visitIAccParallelLoopClause(this);
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
        case 0:  return this.hiddenLiteralStringIndependent;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringIndependent = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

