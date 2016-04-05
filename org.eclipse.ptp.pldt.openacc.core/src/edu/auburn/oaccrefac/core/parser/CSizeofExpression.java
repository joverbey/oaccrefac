package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CSizeofExpression extends ASTNode implements ICExpression
{
    Token hiddenLiteralStringSizeof; // in CSizeofExpression
    ICExpression expression; // in CSizeofExpression

    public ICExpression getExpression()
    {
        return this.expression;
    }

    public void setExpression(ICExpression newValue)
    {
        this.expression = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCSizeofExpression(this);
        visitor.visitICExpression(this);
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
        case 0:  return this.hiddenLiteralStringSizeof;
        case 1:  return this.expression;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringSizeof = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.expression = (ICExpression)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

