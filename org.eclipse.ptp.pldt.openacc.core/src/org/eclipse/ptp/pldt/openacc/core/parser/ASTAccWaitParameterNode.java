package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccWaitParameterNode extends ASTNode
{
    Token hiddenLiteralStringLparen; // in ASTAccWaitParameterNode
    IConstantExpression waitParameter; // in ASTAccWaitParameterNode
    Token hiddenLiteralStringRparen; // in ASTAccWaitParameterNode

    public IConstantExpression getWaitParameter()
    {
        return this.waitParameter;
    }

    public void setWaitParameter(IConstantExpression newValue)
    {
        this.waitParameter = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccWaitParameterNode(this);
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
        case 1:  return this.waitParameter;
        case 2:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.waitParameter = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

