package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccLoopNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccLoopNode
    Token hiddenLiteralStringLoop; // in ASTAccLoopNode
    IASTListNode<ASTAccLoopClauseListNode> accLoopClauseList; // in ASTAccLoopNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccLoopClauseListNode> getAccLoopClauseList()
    {
        return this.accLoopClauseList;
    }

    public void setAccLoopClauseList(IASTListNode<ASTAccLoopClauseListNode> newValue)
    {
        this.accLoopClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccLoopNode(this);
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
        case 1:  return this.hiddenLiteralStringLoop;
        case 2:  return this.accLoopClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLoop = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.accLoopClauseList = (IASTListNode<ASTAccLoopClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

