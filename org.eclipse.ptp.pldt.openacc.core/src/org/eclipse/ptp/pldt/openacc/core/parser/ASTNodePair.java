package org.eclipse.ptp.pldt.openacc.core.parser;

@SuppressWarnings("all")
public class ASTNodePair<T extends IASTNode, U extends IASTNode> extends ASTNode
{
    public final T first;
    public final U second;

    public ASTNodePair(T first, U second)
    {
        assert second != null;

        this.first = first;
        this.second = second;
    }

    @Override protected int getNumASTFields()
    {
        return first == null ? 1 : 2;
    }

    @Override protected IASTNode getASTField(int index)
    {
        if (index == 0)
        {
            return first != null ? first : second;
        }
        else if (index == 1 && first != null)
        {
            return second;
        }
        else throw new IllegalArgumentException();
    }

    @Override protected void setASTField(int index, IASTNode newNode)
    {
        throw new UnsupportedOperationException();
    }

    @Override public void accept(IASTVisitor visitor)
    {
        if (first != null) first.accept(visitor);
        second.accept(visitor);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Source Manipulation
    ///////////////////////////////////////////////////////////////////////////

    @Override public void replaceChild(IASTNode node, IASTNode withNode)
    {
        throw new UnsupportedOperationException();
    }

    @Override public void removeFromTree()
    {
        throw new UnsupportedOperationException();
    }

    @Override public void replaceWith(IASTNode newNode)
    {
        throw new UnsupportedOperationException();
    }

    @Override public void replaceWith(String literalString)
    {
        throw new UnsupportedOperationException();
    }
}
