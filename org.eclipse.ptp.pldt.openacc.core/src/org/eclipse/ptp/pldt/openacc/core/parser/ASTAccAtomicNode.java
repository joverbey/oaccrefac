package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTAccAtomicNode extends ASTNode implements IAccConstruct
{
    Token pragmaAcc; // in ASTAccAtomicNode
    Token hiddenLiteralStringAtomic; // in ASTAccAtomicNode
    IAccAtomicClause accAtomicClause; // in ASTAccAtomicNode

    public Token getPragmaAcc()
    {
        return this.pragmaAcc;
    }

    public void setPragmaAcc(Token newValue)
    {
        this.pragmaAcc = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IAccAtomicClause getAccAtomicClause()
    {
        return this.accAtomicClause;
    }

    public void setAccAtomicClause(IAccAtomicClause newValue)
    {
        this.accAtomicClause = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTAccAtomicNode(this);
        visitor.visitIAccConstruct(this);
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
        case 0:  return this.pragmaAcc;
        case 1:  return this.hiddenLiteralStringAtomic;
        case 2:  return this.accAtomicClause;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.pragmaAcc = (Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenLiteralStringAtomic = (Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.accAtomicClause = (IAccAtomicClause)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

