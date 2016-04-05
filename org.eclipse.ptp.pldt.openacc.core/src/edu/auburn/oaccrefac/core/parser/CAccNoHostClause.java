package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CAccNoHostClause extends ASTNode implements IAccRoutineClause
{
    Token hiddenLiteralStringNohost; // in CAccNoHostClause

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCAccNoHostClause(this);
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
        case 0:  return this.hiddenLiteralStringNohost;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringNohost = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

