package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CAccAutoClause extends ASTNode implements IAccKernelsLoopClause, IAccLoopClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringAuto; // in CAccAutoClause

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCAccAutoClause(this);
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
        case 0:  return this.hiddenLiteralStringAuto;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringAuto = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

