package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccDeclareNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccDeclareNode
    Token hiddenLiteralStringDeclare; // in ASTAccDeclareNode
    IASTListNode<ASTAccDeclareClauseListNode> accDeclareClauseList; // in ASTAccDeclareNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccDeclareClauseListNode> getAccDeclareClauseList()
    {
        return this.accDeclareClauseList;
    }

    public void setAccDeclareClauseList(IASTListNode<ASTAccDeclareClauseListNode> newValue)
    {
        this.accDeclareClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccDeclareNode(this);
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
        case 1:  return this.hiddenLiteralStringDeclare;
        case 2:  return this.accDeclareClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringDeclare = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.accDeclareClauseList = (IASTListNode<ASTAccDeclareClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

