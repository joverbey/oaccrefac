package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccKernelsClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccKernelsClauseListNode
    IAccKernelsClause accKernelsClause; // in ASTAccKernelsClauseListNode

    public IAccKernelsClause getAccKernelsClause()
    {
        return this.accKernelsClause;
    }

    public void setAccKernelsClause(IAccKernelsClause newValue)
    {
        this.accKernelsClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccKernelsClauseListNode(this);
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
        case 1:  return this.accKernelsClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accKernelsClause = (IAccKernelsClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

