package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccCacheNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccCacheNode
    Token hiddenLiteralStringCache; // in ASTAccCacheNode
    Token hiddenLiteralStringLparen; // in ASTAccCacheNode
    IASTListNode<ASTAccDataItemNode> accDataList; // in ASTAccCacheNode
    Token hiddenLiteralStringRparen; // in ASTAccCacheNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccDataItemNode> getAccDataList()
    {
        return this.accDataList;
    }

    public void setAccDataList(IASTListNode<ASTAccDataItemNode> newValue)
    {
        this.accDataList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccCacheNode(this);
        visitor.visitIAccConstruct(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 5;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.pragmaAcc;
        case 1:  return this.hiddenLiteralStringCache;
        case 2:  return this.hiddenLiteralStringLparen;
        case 3:  return this.accDataList;
        case 4:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringCache = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.accDataList = (IASTListNode<ASTAccDataItemNode>)value; if (value != null) value.setParent(this); return;
        case 4:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

