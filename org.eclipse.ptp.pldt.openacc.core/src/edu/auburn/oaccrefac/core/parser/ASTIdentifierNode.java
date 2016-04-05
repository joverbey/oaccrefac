package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTIdentifierNode extends ASTNode
{
    Token identifier; // in ASTIdentifierNode

    public Token getIdentifier()
    {
        return this.identifier;
    }

    public void setIdentifier(Token newValue)
    {
        this.identifier = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTIdentifierNode(this);
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
        case 0:  return this.identifier;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.identifier = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

