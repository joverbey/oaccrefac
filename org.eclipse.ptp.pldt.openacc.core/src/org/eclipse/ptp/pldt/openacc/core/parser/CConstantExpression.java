package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class CConstantExpression extends ASTNode implements ICExpression
{
    Token constant; // in CConstantExpression

    public Token getConstant()
    {
        return this.constant;
    }

    public void setConstant(Token newValue)
    {
        this.constant = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCConstantExpression(this);
        visitor.visitICExpression(this);
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
        case 0:  return this.constant;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.constant = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

