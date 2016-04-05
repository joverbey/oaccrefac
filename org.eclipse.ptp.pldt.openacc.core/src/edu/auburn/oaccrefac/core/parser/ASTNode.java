package edu.auburn.oaccrefac.core.parser;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;

import edu.auburn.oaccrefac.core.parser.ASTNodeUtil.NonNullIterator;

@SuppressWarnings("all")
public abstract class ASTNode implements IASTNode
{
    private IASTNode parent = null;

    ///////////////////////////////////////////////////////////////////////////
    // Traversal and Visitor Support
    ///////////////////////////////////////////////////////////////////////////

    @Override public IASTNode getParent()
    {
        return this.parent;
    }

    @Override public void setParent(IASTNode parent)
    {
        this.parent = parent;
    }

    @Override public Iterable<? extends IASTNode> getChildren()
    {
        return new Iterable<IASTNode>()
        {
        	@Override public Iterator<IASTNode> iterator()
            {
                return new NonNullIterator<IASTNode>(new Iterator<IASTNode>()
                {
                    private int index = 0, numChildren = getNumASTFields();

                    @Override public boolean hasNext()
                    {
                        return index < numChildren;
                    }

                    @Override public IASTNode next()
                    {
                        return getASTField(index++);
                    }

                    @Override public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                });
            }
        };
    }

    protected abstract int getNumASTFields();

    protected abstract IASTNode getASTField(int index);

    protected abstract void setASTField(int index, IASTNode value);

    @Override public abstract void accept(IASTVisitor visitor);

    ///////////////////////////////////////////////////////////////////////////
    // Source Manipulation
    ///////////////////////////////////////////////////////////////////////////

    @Override public void replaceChild(IASTNode node, IASTNode withNode)
    {
        for (int i = 0; i < getNumASTFields(); i++)
        {
            if (getASTField(i) == node)
            {
                setASTField(i, withNode);
                if (withNode != null) withNode.setParent(this);
                // if (node != null) node.setParent(null);
                return;
            }
        }

        throw new IllegalStateException("Child node not found");
    }

    @Override public void removeFromTree()
    {
        ASTNodeUtil.removeFromTree(this);
    }

    @Override public void replaceWith(IASTNode newNode)
    {
        ASTNodeUtil.replaceWith(this, newNode);
    }

    @Override public void replaceWith(String literalString)
    {
        ASTNodeUtil.replaceWith(this, literalString);
    }

    @Override public Object clone()
    {
        try
        {
            ASTNode copy = (ASTNode)super.clone();
            for (int i = 0; i < getNumASTFields(); i++)
            {
                if (getASTField(i) != null)
                {
                    IASTNode newChild = (IASTNode)getASTField(i).clone();
                    newChild.setParent(copy);
                    copy.setASTField(i, newChild);
                }
            }
            return copy;
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Searching
    ///////////////////////////////////////////////////////////////////////////

    @Override public <T extends IASTNode> Set<T> findAll(Class<T> targetClass)
    {
        return ASTNodeUtil.findAll(this, targetClass);
    }

    @Override public <T extends IASTNode> T findNearestAncestor(Class<T> targetClass)
    {
        return ASTNodeUtil.findNearestAncestor(this, targetClass);
    }

    @Override public <T extends IASTNode> T findFirst(Class<T> targetClass)
    {
        return ASTNodeUtil.findFirst(this, targetClass);
    }

    @Override public <T extends IASTNode> T findLast(Class<T> targetClass)
    {
        return ASTNodeUtil.findLast(this, targetClass);
    }

    @Override public Token findFirstToken()
    {
        return ASTNodeUtil.findFirstToken(this);
    }

    @Override public Token findLastToken()
    {
        return ASTNodeUtil.findLastToken(this);
    }

    @Override public boolean isFirstChildInList()
    {
        return ASTNodeUtil.isFirstChildInList(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Source Reproduction
    ///////////////////////////////////////////////////////////////////////////

    @Override public void printOn(PrintStream out)
    {
        ASTNodeUtil.print(this, out);
    }

    @Override public String toString()
    {
        return ASTNodeUtil.toString(this);
    }
}
