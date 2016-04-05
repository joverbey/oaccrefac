package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccWaitNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccWaitNode
    Token hiddenLiteralStringWait; // in ASTAccWaitNode
    Token hiddenLiteralStringLparen; // in ASTAccWaitNode
    IConstantExpression waitParameter; // in ASTAccWaitNode
    Token hiddenLiteralStringRparen; // in ASTAccWaitNode
    IASTListNode<ASTAccWaitClauseListNode> accWaitClauseList; // in ASTAccWaitNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IConstantExpression getWaitParameter()
    {
        return this.waitParameter;
    }

    public void setWaitParameter(IConstantExpression newValue)
    {
        this.waitParameter = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccWaitClauseListNode> getAccWaitClauseList()
    {
        return this.accWaitClauseList;
    }

    public void setAccWaitClauseList(IASTListNode<ASTAccWaitClauseListNode> newValue)
    {
        this.accWaitClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccWaitNode(this);
        visitor.visitIAccConstruct(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 6;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.pragmaAcc;
        case 1:  return this.hiddenLiteralStringWait;
        case 2:  return this.hiddenLiteralStringLparen;
        case 3:  return this.waitParameter;
        case 4:  return this.hiddenLiteralStringRparen;
        case 5:  return this.accWaitClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringWait = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.waitParameter = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 4:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        case 5:  this.accWaitClauseList = (IASTListNode<ASTAccWaitClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

