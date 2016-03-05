package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccPresentorcopyinClauseNode extends ASTNode implements IAccDataClause, IAccDeclareClause, IAccEnterDataClause, IAccKernelsClause, IAccKernelsLoopClause, IAccParallelClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringPcopyin; // in ASTAccPresentorcopyinClauseNode
    Token hiddenLiteralStringPresentUnderscoreorUnderscorecopyin; // in ASTAccPresentorcopyinClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccPresentorcopyinClauseNode
    IASTListNode<ASTAccDataItemNode> accDataList; // in ASTAccPresentorcopyinClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccPresentorcopyinClauseNode

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
        visitor.visitASTAccPresentorcopyinClauseNode(this);
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
        return 5;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.hiddenLiteralStringPcopyin;
        case 1:  return this.hiddenLiteralStringPresentUnderscoreorUnderscorecopyin;
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
        case 0:  this.hiddenLiteralStringPcopyin = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringPresentUnderscoreorUnderscorecopyin = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.accDataList = (IASTListNode<ASTAccDataItemNode>)value; if (value != null) value.setParent(this); return;
        case 4:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

