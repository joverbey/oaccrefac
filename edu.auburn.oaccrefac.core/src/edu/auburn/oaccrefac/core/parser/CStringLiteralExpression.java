package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CStringLiteralExpression extends ASTNode implements ICExpression
{
    IASTListNode<Token> literals; // in CStringLiteralExpression

    public IASTListNode<Token> getLiterals()
    {
        return this.literals;
    }

    public void setLiterals(IASTListNode<Token> newValue)
    {
        this.literals = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCStringLiteralExpression(this);
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
        case 0:  return this.literals;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.literals = (IASTListNode<Token>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

