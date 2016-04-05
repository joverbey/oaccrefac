package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class CIdentifierExpression extends ASTNode implements ICExpression
{
    ASTIdentifierNode identifier; // in CIdentifierExpression

    public ASTIdentifierNode getIdentifier()
    {
        return this.identifier;
    }

    public void setIdentifier(ASTIdentifierNode newValue)
    {
        this.identifier = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCIdentifierExpression(this);
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
        case 0:  return this.identifier;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.identifier = (ASTIdentifierNode)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

