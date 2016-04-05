package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class CAccAtomicWriteClause extends ASTNode implements IAccAtomicClause
{
    Token hiddenLiteralStringWrite; // in CAccAtomicWriteClause

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCAccAtomicWriteClause(this);
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
        case 0:  return this.hiddenLiteralStringWrite;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringWrite = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

