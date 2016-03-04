package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccNoConstruct extends ASTNode implements IAccConstruct
{

    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccNoConstruct(this);
        visitor.visitIAccConstruct(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 0;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

