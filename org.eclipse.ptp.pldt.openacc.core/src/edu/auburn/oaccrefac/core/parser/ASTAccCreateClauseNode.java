package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccCreateClauseNode extends ASTNode implements IAccDataClause, IAccDeclareClause, IAccEnterDataClause, IAccKernelsClause, IAccKernelsLoopClause, IAccParallelClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringCreate; // in ASTAccCreateClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccCreateClauseNode
    IASTListNode<ASTAccDataItemNode> accDataList; // in ASTAccCreateClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccCreateClauseNode

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
        visitor.visitASTAccCreateClauseNode(this);
        visitor.visitIAccDataClause(this);
        visitor.visitIAccDeclareClause(this);
        visitor.visitIAccEnterDataClause(this);
        visitor.visitIAccKernelsClause(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccParallelClause(this);
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
        case 0:  return this.hiddenLiteralStringCreate;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.accDataList;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringCreate = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.accDataList = (IASTListNode<ASTAccDataItemNode>)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

