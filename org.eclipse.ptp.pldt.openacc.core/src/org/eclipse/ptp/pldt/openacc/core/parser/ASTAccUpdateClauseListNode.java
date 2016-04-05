package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccUpdateClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccUpdateClauseListNode
    IAccUpdateClause accUpdateClause; // in ASTAccUpdateClauseListNode

    public IAccUpdateClause getAccUpdateClause()
    {
        return this.accUpdateClause;
    }

    public void setAccUpdateClause(IAccUpdateClause newValue)
    {
        this.accUpdateClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccUpdateClauseListNode(this);
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
        case 0:  return this.hiddenLiteralStringComma;
        case 1:  return this.accUpdateClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accUpdateClause = (IAccUpdateClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}
