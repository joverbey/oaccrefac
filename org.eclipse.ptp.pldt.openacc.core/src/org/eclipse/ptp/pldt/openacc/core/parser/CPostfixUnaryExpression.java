package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class CPostfixUnaryExpression extends ASTNode implements IAssignmentExpression, ICExpression, IConstantExpression
{
    ICExpression subexpression; // in CPostfixUnaryExpression
    Token operator; // in CPostfixUnaryExpression

    public ICExpression getSubexpression()
    {
        return this.subexpression;
    }

    public void setSubexpression(ICExpression newValue)
    {
        this.subexpression = newValue;
        if (newValue != null) newValue.setParent(this);
    }


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
        visitor.visitCPostfixUnaryExpression(this);
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
        case 0:  return this.subexpression;
        case 1:  return this.operator;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.subexpression = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 1:  this.operator = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

