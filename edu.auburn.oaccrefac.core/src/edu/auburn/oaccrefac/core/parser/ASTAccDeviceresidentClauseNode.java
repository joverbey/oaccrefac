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
public class ASTAccDeviceresidentClauseNode extends ASTNode implements IAccDeclareClause
{
    Token hiddenLiteralStringDeviceUnderscoreresident; // in ASTAccDeviceresidentClauseNode
    Token hiddenLiteralStringLparen; // in ASTAccDeviceresidentClauseNode
    IASTListNode<ASTIdentifierNode> identifierList; // in ASTAccDeviceresidentClauseNode
    Token hiddenLiteralStringRparen; // in ASTAccDeviceresidentClauseNode

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
        visitor.visitASTAccDeviceresidentClauseNode(this);
        visitor.visitIAccDeclareClause(this);
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
        case 0:  return this.hiddenLiteralStringDeviceUnderscoreresident;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.identifierList;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringDeviceUnderscoreresident = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.identifierList = (IASTListNode<ASTIdentifierNode>)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

