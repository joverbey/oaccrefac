package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccPresentorcreateClauseNode extends ASTNode implements IAccDataClause, IAccDeclareClause, IAccEnterDataClause, IAccKernelsClause, IAccKernelsLoopClause, IAccParallelClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringPresentUnderscoreorUnderscorecreate; // in ASTAccPresentorcreateClauseNode
    Token hiddenLiteralStringPcreate; // in ASTAccPresentorcreateClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccPresentorcreateClauseNode
    IASTListNode<ASTAccDataItemNode> accDataList; // in ASTAccPresentorcreateClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccPresentorcreateClauseNode

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
        visitor.visitASTAccPresentorcreateClauseNode(this);
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
        case 0:  return this.hiddenLiteralStringPresentUnderscoreorUnderscorecreate;
        case 1:  return this.hiddenLiteralStringPcreate;
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
        case 0:  this.hiddenLiteralStringPresentUnderscoreorUnderscorecreate = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringPcreate = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.accDataList = (IASTListNode<ASTAccDataItemNode>)value; if (value != null) value.setParent(this); return;
        case 4:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

