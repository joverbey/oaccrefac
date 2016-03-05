package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccCollapseClauseNode extends ASTNode implements IAccKernelsLoopClause, IAccLoopClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringCollapse; // in ASTAccCollapseClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccCollapseClauseNode
    IConstantExpression count; // in ASTAccCollapseClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccCollapseClauseNode

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
        visitor.visitASTAccCollapseClauseNode(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccLoopClause(this);
        visitor.visitIAccParallelLoopClause(this);
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
        case 0:  return this.hiddenLiteralStringCollapse;
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
        case 0:  this.hiddenLiteralStringCollapse = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.count = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

