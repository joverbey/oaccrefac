package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccDataNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccDataNode
    Token hiddenLiteralStringData; // in ASTAccDataNode
    IASTListNode<ASTAccDataClauseListNode> accDataClauseList; // in ASTAccDataNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccDataClauseListNode> getAccDataClauseList()
    {
        return this.accDataClauseList;
    }

    public void setAccDataClauseList(IASTListNode<ASTAccDataClauseListNode> newValue)
    {
        this.accDataClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccDataNode(this);
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
        case 1:  return this.hiddenLiteralStringData;
        case 2:  return this.accDataClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringData = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.accDataClauseList = (IASTListNode<ASTAccDataClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

