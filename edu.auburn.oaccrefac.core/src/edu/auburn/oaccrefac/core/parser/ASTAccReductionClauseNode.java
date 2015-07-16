package edu.auburn.oaccrefac.core.parser;

import java.io.PrintStream;
import java.util.Iterator;

import java.util.List;

import edu.auburn.oaccrefac.core.parser.ASTListNode;
import edu.auburn.oaccrefac.core.parser.ASTNode;
import edu.auburn.oaccrefac.core.parser.ASTNodeWithErrorRecoverySymbols;
import edu.auburn.oaccrefac.core.parser.IASTListNode;
import edu.auburn.oaccrefac.core.parser.IASTNode;
import edu.auburn.oaccrefac.core.parser.IASTVisitor;
import edu.auburn.oaccrefac.core.parser.Token;

import edu.auburn.oaccrefac.core.parser.SyntaxException;                   import java.io.IOException;

@SuppressWarnings("all")
public class ASTAccReductionClauseNode extends ASTNode implements IAccKernelsLoopClause, IAccLoopClause, IAccParallelClause, IAccParallelLoopClause
{
    Token hiddenLiteralStringReduction; // in ASTAccReductionClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccReductionClauseNode
    Token operator; // in ASTAccReductionClauseNode
    Token hiddenLiteralStringColon; // in ASTAccReductionClauseNode
    IASTListNode<ASTIdentifierNode> identifierList; // in ASTAccReductionClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccReductionClauseNode

    public Token getOperator()
    {
        return this.operator;
    }

    public void setOperator(Token newValue)
    {
        this.operator = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTIdentifierNode> getIdentifierList()
    {
        return this.identifierList;
    }

    public void setIdentifierList(IASTListNode<ASTIdentifierNode> newValue)
    {
        this.identifierList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccReductionClauseNode(this);
        visitor.visitIAccKernelsLoopClause(this);
        visitor.visitIAccLoopClause(this);
        visitor.visitIAccParallelClause(this);
        visitor.visitIAccParallelLoopClause(this);
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
        case 0:  return this.hiddenLiteralStringReduction;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.operator;
        case 3:  return this.hiddenLiteralStringColon;
        case 4:  return this.identifierList;
        case 5:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringReduction = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.operator = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringColon = (Token)value; if (value != null) value.setParent(this); return;
        case 4:  this.identifierList = (IASTListNode<ASTIdentifierNode>)value; if (value != null) value.setParent(this); return;
        case 5:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

