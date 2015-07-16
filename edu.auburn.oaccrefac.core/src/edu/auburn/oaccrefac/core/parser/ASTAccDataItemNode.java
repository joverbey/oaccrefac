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
public class ASTAccDataItemNode extends ASTNode
{
    ASTIdentifierNode identifier; // in ASTAccDataItemNode
    Token hiddenLiteralStringLbracket; // in ASTAccDataItemNode
    IConstantExpression lowerBound; // in ASTAccDataItemNode
    Token hiddenLiteralStringColon; // in ASTAccDataItemNode
    IConstantExpression count; // in ASTAccDataItemNode
    Token hiddenLiteralStringRbracket; // in ASTAccDataItemNode

    public ASTIdentifierNode getIdentifier()
    {
        return this.identifier;
    }

    public void setIdentifier(ASTIdentifierNode newValue)
    {
        this.identifier = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IConstantExpression getLowerBound()
    {
        return this.lowerBound;
    }

    public void setLowerBound(IConstantExpression newValue)
    {
        this.lowerBound = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IConstantExpression getCount()
    {
        return this.count;
    }

    public void setCount(IConstantExpression newValue)
    {
        this.count = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccDataItemNode(this);
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
        case 0:  return this.identifier;
        case 1:  return this.hiddenLiteralStringLbracket;
        case 2:  return this.lowerBound;
        case 3:  return this.hiddenLiteralStringColon;
        case 4:  return this.count;
        case 5:  return this.hiddenLiteralStringRbracket;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.identifier = (ASTIdentifierNode)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLbracket = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.lowerBound = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringColon = (Token)value; if (value != null) value.setParent(this); return;
        case 4:  this.count = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 5:  this.hiddenLiteralStringRbracket = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

