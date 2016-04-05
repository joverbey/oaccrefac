package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CPrefixUnaryExpression extends ASTNode implements IAssignmentExpression, ICExpression, IConstantExpression
{
    Token operator; // in CPrefixUnaryExpression
    ICExpression subexpression; // in CPrefixUnaryExpression

    public Token getOperator()
    {
        return this.operator;
    }

    public void setOperator(Token newValue)
    {
        this.operator = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ICExpression getSubexpression()
    {
        return this.subexpression;
    }

    public void setSubexpression(ICExpression newValue)
    {
        this.subexpression = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCPrefixUnaryExpression(this);
        visitor.visitIAssignmentExpression(this);
        visitor.visitICExpression(this);
        visitor.visitIConstantExpression(this);
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
        case 0:  return this.operator;
        case 1:  return this.subexpression;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.operator = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.subexpression = (ICExpression)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

