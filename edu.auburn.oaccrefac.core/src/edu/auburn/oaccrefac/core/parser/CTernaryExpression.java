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
public class CTernaryExpression extends ASTNode implements ICExpression
{
    ICExpression testExpression; // in CTernaryExpression
    Token hiddenLiteralStringQuestion; // in CTernaryExpression
    ASTExpressionNode thenExpression; // in CTernaryExpression
    Token hiddenLiteralStringColon; // in CTernaryExpression
    ICExpression elseExpression; // in CTernaryExpression

    public ICExpression getTestExpression()
    {
        return this.testExpression;
    }

    public void setTestExpression(ICExpression newValue)
    {
        this.testExpression = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ASTExpressionNode getThenExpression()
    {
        return this.thenExpression;
    }

    public void setThenExpression(ASTExpressionNode newValue)
    {
        this.thenExpression = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ICExpression getElseExpression()
    {
        return this.elseExpression;
    }

    public void setElseExpression(ICExpression newValue)
    {
        this.elseExpression = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCTernaryExpression(this);
        visitor.visitICExpression(this);
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
        case 0:  return this.testExpression;
        case 1:  return this.hiddenLiteralStringQuestion;
        case 2:  return this.thenExpression;
        case 3:  return this.hiddenLiteralStringColon;
        case 4:  return this.elseExpression;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.testExpression = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringQuestion = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.thenExpression = (ASTExpressionNode)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringColon = (Token)value; if (value != null) value.setParent(this); return;
        case 4:  this.elseExpression = (ICExpression)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

