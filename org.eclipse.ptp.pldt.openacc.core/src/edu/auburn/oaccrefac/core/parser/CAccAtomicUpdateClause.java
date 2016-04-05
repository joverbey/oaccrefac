package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CAccAtomicUpdateClause extends ASTNode implements IAccAtomicClause
{
    Token hiddenLiteralStringUpdate; // in CAccAtomicUpdateClause

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCAccAtomicUpdateClause(this);
        visitor.visitIAccAtomicClause(this);
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
        case 0:  return this.hiddenLiteralStringUpdate;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringUpdate = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

