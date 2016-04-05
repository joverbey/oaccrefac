package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class CAccAtomicReadClause extends ASTNode implements IAccAtomicClause
{
    Token hiddenLiteralStringRead; // in CAccAtomicReadClause

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCAccAtomicReadClause(this);
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
        case 0:  return this.hiddenLiteralStringRead;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringRead = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}
