package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccVectorClauseNode extends ASTNode implements IAccKernelsLoopClause, IAccLoopClause, IAccParallelLoopClause, IAccRoutineClause
{
    Token hiddenLiteralStringVector; // in ASTAccVectorClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccVectorClauseNode
    IConstantExpression count; // in ASTAccVectorClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccVectorClauseNode

    public IConstantExpression getCount()
    {
        return this.count;
    }

    public void setCount(IConstantExpression newValue)
    {
        this.count = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccVectorClauseNode(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccLoopClause(this);
        visitor.visitIAccParallelLoopClause(this);
        visitor.visitIAccRoutineClause(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 4;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.hiddenLiteralStringVector;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.count;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringVector = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.count = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}
