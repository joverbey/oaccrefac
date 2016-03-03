package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccHostdataNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccHostdataNode
    Token hiddenLiteralStringHostUnderscoredata; // in ASTAccHostdataNode
    IASTListNode<ASTAccHostdataClauseListNode> accHostdataClauseList; // in ASTAccHostdataNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccHostdataClauseListNode> getAccHostdataClauseList()
    {
        return this.accHostdataClauseList;
    }

    public void setAccHostdataClauseList(IASTListNode<ASTAccHostdataClauseListNode> newValue)
    {
        this.accHostdataClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccHostdataNode(this);
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
        case 1:  return this.hiddenLiteralStringHostUnderscoredata;
        case 2:  return this.accHostdataClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringHostUnderscoredata = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.accHostdataClauseList = (IASTListNode<ASTAccHostdataClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

