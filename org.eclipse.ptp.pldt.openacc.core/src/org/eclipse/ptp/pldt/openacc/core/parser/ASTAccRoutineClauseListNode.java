package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccRoutineClauseListNode extends ASTNode
{
    Token hiddenLiteralStringComma; // in ASTAccRoutineClauseListNode
    IAccRoutineClause accRoutineClause; // in ASTAccRoutineClauseListNode

    public IAccRoutineClause getAccRoutineClause()
    {
        return this.accRoutineClause;
    }

    public void setAccRoutineClause(IAccRoutineClause newValue)
    {
        this.accRoutineClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccRoutineClauseListNode(this);
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
        case 1:  return this.accRoutineClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringComma = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.accRoutineClause = (IAccRoutineClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

