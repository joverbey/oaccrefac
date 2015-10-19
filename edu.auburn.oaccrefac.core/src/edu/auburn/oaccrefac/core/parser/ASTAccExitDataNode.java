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
public class ASTAccExitDataNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccExitDataNode
    Token hiddenLiteralStringExit; // in ASTAccExitDataNode
    Token hiddenLiteralStringData; // in ASTAccExitDataNode
    IASTListNode<ASTAccExitDataClauseListNode> accExitDataClauseList; // in ASTAccExitDataNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTAccExitDataClauseListNode> getAccExitDataClauseList()
    {
        return this.accExitDataClauseList;
    }

    public void setAccExitDataClauseList(IASTListNode<ASTAccExitDataClauseListNode> newValue)
    {
        this.accExitDataClauseList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccExitDataNode(this);
        visitor.visitIAccConstruct(this);
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
        case 0:  return this.pragmaAcc;
        case 1:  return this.hiddenLiteralStringExit;
        case 2:  return this.hiddenLiteralStringData;
        case 3:  return this.accExitDataClauseList;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringExit = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenLiteralStringData = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.accExitDataClauseList = (IASTListNode<ASTAccExitDataClauseListNode>)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

