package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccParallelNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccParallelNode
    Token hiddenLiteralStringParallel; // in ASTAccParallelNode
    IASTListNode<ASTAccParallelClauseListNode> accParallelClauseList; // in ASTAccParallelNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccParallelClauseListNode> getAccParallelClauseList()
    {
        return this.accParallelClauseList;
    }

    public void setAccParallelClauseList(IASTListNode<ASTAccParallelClauseListNode> newValue)
    {
        this.accParallelClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccParallelNode(this);
        visitor.visitIAccConstruct(this);
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
        case 0:  return this.pragmaAcc;
        case 1:  return this.hiddenLiteralStringParallel;
        case 2:  return this.accParallelClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringParallel = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.accParallelClauseList = (IASTListNode<ASTAccParallelClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

