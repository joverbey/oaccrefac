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
public class CElementAccessExpression extends ASTNode implements IAssignmentExpression, ICExpression, IConstantExpression
{
    ICExpression structure; // in CElementAccessExpression
    Token hiddenLiteralStringPeriod; // in CElementAccessExpression
    Token arrow; // in CElementAccessExpression
    ASTIdentifierNode identifier; // in CElementAccessExpression

    public ICExpression getStructure()
    {
        return this.structure;
    }

    public void setStructure(ICExpression newValue)
    {
        this.structure = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public boolean arrow()
    {
        return this.arrow != null;
    }

    public void setArrow(Token newValue)
    {
        this.arrow = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ASTIdentifierNode getIdentifier()
    {
        return this.identifier;
    }

    public void setIdentifier(ASTIdentifierNode newValue)
    {
        this.identifier = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCElementAccessExpression(this);
        visitor.visitIAssignmentExpression(this);
        visitor.visitICExpression(this);
        visitor.visitIConstantExpression(this);
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
        case 0:  return this.structure;
        case 1:  return this.hiddenLiteralStringPeriod;
        case 2:  return this.arrow;
        case 3:  return this.identifier;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.structure = (ICExpression)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringPeriod = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.arrow = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.identifier = (ASTIdentifierNode)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

