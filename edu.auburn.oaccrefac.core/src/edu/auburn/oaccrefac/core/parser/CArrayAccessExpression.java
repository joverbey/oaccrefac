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
public class CArrayAccessExpression extends ASTNode implements ICExpression
{
    ICExpression array; // in CArrayAccessExpression
    Token hiddenLiteralStringLbracket; // in CArrayAccessExpression
    ASTExpressionNode subscript; // in CArrayAccessExpression
    Token hiddenLiteralStringRbracket; // in CArrayAccessExpression

    public ICExpression getArray()
    {
        return this.array;
    }

    public void setArray(ICExpression newValue)
    {
        this.array = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ASTExpressionNode getSubscript()
    {
        return this.subscript;
    }

    public void setSubscript(ASTExpressionNode newValue)
    {
        this.subscript = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCArrayAccessExpression(this);
        visitor.visitICExpression(this);
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
        case 0:  return this.array;
        case 1:  return this.hiddenLiteralStringLbracket;
        case 2:  return this.subscript;
        case 3:  return this.hiddenLiteralStringRbracket;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.array = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLbracket = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.subscript = (ASTExpressionNode)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRbracket = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

