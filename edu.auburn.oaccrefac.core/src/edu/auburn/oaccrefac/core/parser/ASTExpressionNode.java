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
public class ASTExpressionNode extends ASTNode implements ICExpression
{
    private IASTListNode<Token> prependedTokens = new ASTListNode<Token>();
    private IASTListNode<Token> appendedTokens = new ASTListNode<Token>();
    ICExpression conditionalExpression; // in ASTExpressionNode

    public ICExpression getConditionalExpression()
    {
        return this.conditionalExpression;
    }

    public void setConditionalExpression(ICExpression newValue)
    {
        this.conditionalExpression = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTExpressionNode(this);
        visitor.visitICExpression(this);
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
        case 0:  return this.prependedTokens;
        case 1:  return this.conditionalExpression;
        case 2:  return this.appendedTokens;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.prependedTokens = (IASTListNode<Token>)value; if (value != null) value.setParent(this); return;
        case 1:  this.conditionalExpression = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 2:  this.appendedTokens = (IASTListNode<Token>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    IASTListNode<Token> getPrependedTokens()
    {
        return prependedTokens;
    }

    IASTListNode<Token> getAppendedTokens()
    {
        return appendedTokens;
    }

    public void prependToken(Token token)
    {
        prependedTokens.add(0, token);
        token.setParent(this);
    }

    public void appendToken(Token token)
    {
        appendedTokens.add(token);
        token.setParent(this);
    }
}

