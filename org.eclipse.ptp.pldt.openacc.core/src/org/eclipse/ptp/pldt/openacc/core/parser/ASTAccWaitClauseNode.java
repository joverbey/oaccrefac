package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccWaitClauseNode extends ASTNode implements IAccEnterDataClause, IAccExitDataClause, IAccKernelsClause, IAccKernelsLoopClause, IAccParallelClause, IAccParallelLoopClause, IAccUpdateClause
{
    Token hiddenLiteralStringWait; // in ASTAccWaitClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccWaitClauseNode
    IASTListNode<IAssignmentExpression> argList; // in ASTAccWaitClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccWaitClauseNode

    public IASTListNode<IAssignmentExpression> getArgList()
    {
        return this.argList;
    }

    public void setArgList(IASTListNode<IAssignmentExpression> newValue)
    {
        this.argList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccWaitClauseNode(this);
        visitor.visitIAccEnterDataClause(this);
        visitor.visitIAccExitDataClause(this);
        visitor.visitIAccKernelsClause(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccParallelClause(this);
        visitor.visitIAccParallelLoopClause(this);
        visitor.visitIAccUpdateClause(this);
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
        case 0:  return this.hiddenLiteralStringWait;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.argList;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringWait = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.argList = (IASTListNode<IAssignmentExpression>)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

