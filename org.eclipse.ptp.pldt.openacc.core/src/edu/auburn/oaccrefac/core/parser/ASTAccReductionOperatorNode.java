package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccReductionOperatorNode extends ASTNode
{
    Token operator; // in ASTAccReductionOperatorNode

    public Token getOperator()
    {
        return this.operator;
    }

    public void setOperator(Token newValue)
    {
        this.operator = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccReductionOperatorNode(this);
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
        case 0:  return this.operator;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.operator = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

