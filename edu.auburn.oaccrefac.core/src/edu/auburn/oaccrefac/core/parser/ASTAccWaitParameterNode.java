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
public class ASTAccWaitParameterNode extends ASTNode
{
    Token hiddenLiteralStringLparen; // in ASTAccWaitParameterNode
    IConstantExpression waitParameter; // in ASTAccWaitParameterNode
    Token hiddenLiteralStringRparen; // in ASTAccWaitParameterNode

    public IConstantExpression getWaitParameter()
    {
        return this.waitParameter;
    }

    public void setWaitParameter(IConstantExpression newValue)
    {
        this.waitParameter = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccWaitParameterNode(this);
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
        case 0:  return this.hiddenLiteralStringLparen;
        case 1:  return this.waitParameter;
        case 2:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.waitParameter = (IConstantExpression)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

