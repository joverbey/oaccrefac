package edu.auburn.oaccrefac.core.parser;

@SuppressWarnings("all")
public class CAccBindClause extends ASTNode implements IAccRoutineClause
{
    Token hiddenLiteralStringBind; // in CAccBindClause
    Token hiddenLiteralStringLparen; // in CAccBindClause
    Token name; // in CAccBindClause
    Token hiddenLiteralStringRparen; // in CAccBindClause

    public Token getName()
    {
        return this.name;
    }

    public void setName(Token newValue)
    {
        this.name = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitCAccBindClause(this);
        visitor.visitIAccRoutineClause(this);
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
        case 0:  return this.hiddenLiteralStringBind;
        case 1:  return this.hiddenLiteralStringLparen;
        case 2:  return this.name;
        case 3:  return this.hiddenLiteralStringRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.hiddenLiteralStringBind = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringLparen = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.name = (Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenLiteralStringRparen = (Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

