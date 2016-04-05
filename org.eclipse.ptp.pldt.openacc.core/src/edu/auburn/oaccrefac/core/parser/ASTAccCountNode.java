package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccCountNode extends ASTNode
{
    Token hiddenLiteralStringLparen; // in ASTAccCountNode
    IConstantExpression count; // in ASTAccCountNode
    Token hiddenLiteralStringRparen; // in ASTAccCountNode

    public IConstantExpression getCount()
    {
        return this.count;
    }

    public void setCount(IConstantExpression newValue)
    {
        this.count = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccCountNode(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 3;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.hiddenLiteralStringLparen;
        case 1:  return this.count;
        case 2:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.count = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

