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
public class CBinaryExpression extends ASTNode implements IAssignmentExpression, ICExpression, IConstantExpression
{
    ICExpression lhs; // in CBinaryExpression
    Token operator; // in CBinaryExpression
    ICExpression rhs; // in CBinaryExpression

    public ICExpression getLhs()
    {
        return this.lhs;
    }

    public void setLhs(ICExpression newValue)
    {
        this.lhs = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public Token getOperator()
    {
        return this.operator;
    }

    public void setOperator(Token newValue)
    {
        this.operator = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ICExpression getRhs()
    {
        return this.rhs;
    }

    public void setRhs(ICExpression newValue)
    {
        this.rhs = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCBinaryExpression(this);
        visitor.visitIAssignmentExpression(this);
        visitor.visitICExpression(this);
        visitor.visitIConstantExpression(this);
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
        case 0:  return this.lhs;
        case 1:  return this.operator;
        case 2:  return this.rhs;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.lhs = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 1:  this.operator = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.rhs = (ICExpression)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

