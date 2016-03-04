package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class ASTAccTileClauseNode extends ASTNode implements IAccKernelsLoopClause, IAccLoopClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringTile; // in ASTAccTileClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccTileClauseNode
    IASTListNode<IAssignmentExpression> list; // in ASTAccTileClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccTileClauseNode

    public IASTListNode<IAssignmentExpression> getList()
    {
        return this.list;
    }

    public void setList(IASTListNode<IAssignmentExpression> newValue)
    {
        this.list = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccTileClauseNode(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccLoopClause(this);
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
        case 0:  return this.hiddenLiteralStringTile;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.list;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringTile = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.list = (IASTListNode<IAssignmentExpression>)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

