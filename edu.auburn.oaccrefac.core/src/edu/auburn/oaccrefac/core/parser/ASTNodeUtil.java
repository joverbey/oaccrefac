package edu.auburn.oaccrefac.core.parser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("all")
public final class ASTNodeUtil
{
    private ASTNodeUtil() {}

    public static void removeFromTree(IASTNode node)
    {
        IASTNode parent = node.getParent();
        if (parent == null) throw new IllegalArgumentException("Cannot remove root node");
        parent.replaceChild(node, null);
    }

    public static void replaceWith(IASTNode node, IASTNode newNode)
    {
        IASTNode parent = node.getParent();
        if (parent == null) throw new IllegalArgumentException("Cannot remove root node");
        parent.replaceChild(node, newNode);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IASTNode> Set<T> findAll(IASTNode node, final Class<T> clazz)
    {
        class V extends GenericASTVisitor
        {
            Set<T> result = new HashSet<T>();

            @Override public void visitASTNode(IASTNode node)
            {
                if (clazz.isAssignableFrom(node.getClass()))
                    result.add((T)node);
                traverseChildren(node);
            }

            @Override public void visitToken(Token node)
            {
                if (clazz.isAssignableFrom(node.getClass()))
                    result.add((T)node);
            }
        };

        V v = new V();
        node.accept(v);
        return v.result;
    }

    @SuppressWarnings("unchecked")
    public static <T extends IASTNode> T findNearestAncestor(IASTNode node, Class<T> targetClass)
    {
        for (IASTNode parent = node.getParent(); parent != null; parent = parent.getParent())
            if (targetClass.isAssignableFrom(parent.getClass()))
                return (T)parent;
        return null;
    }

    protected static final class Notification extends Error
    {
        private Object result;

        public Notification(Object result) { this.result = result; }

        public Object getResult() { return result; }
    }

    @SuppressWarnings("unchecked")
    public static <T extends IASTNode> T findFirst(IASTNode node, final Class<T> clazz)
    {
        try
        {
            node.accept(new GenericASTVisitor()
            {
                @Override
                public void visitASTNode(IASTNode node)
                {
                    if (clazz.isAssignableFrom(node.getClass()))
                        throw new Notification(node);
                    traverseChildren(node);
                }

                @Override public void visitToken(Token node)
                {
                    if (clazz.isAssignableFrom(node.getClass()))
                        throw new Notification(node);
                }
            });
            return null;
        }
        catch (Notification n)
        {
            return (T)n.getResult();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends IASTNode> T findLast(IASTNode node, final Class<T> clazz)
    {
        class V extends GenericASTVisitor
        {
            T result = null;

            @Override public void visitASTNode(IASTNode node)
            {
                if (clazz.isAssignableFrom(node.getClass()))
                    result = (T)node;
                traverseChildren(node);
            }

            @Override public void visitToken(Token node)
            {
                if (clazz.isAssignableFrom(node.getClass()))
                    result = (T)node;
            }
        };

        V v = new V();
        node.accept(v);
        return v.result;
    }

    public static Token findFirstToken(IASTNode node)
    {
        return findFirst(node, Token.class);
    }

    public static Token findLastToken(IASTNode node)
    {
        return findLast(node, Token.class);
    }

    public static boolean isFirstChildInList(IASTNode node)
    {
        return node.getParent() != null
            && node.getParent() instanceof IASTListNode
            && ((IASTListNode<?>)node.getParent()).size() > 0
            && ((IASTListNode<?>)node.getParent()).get(0) == node;
    }

    public static String toString(IASTNode node)
    {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        node.printOn(new PrintStream(bs));
        return bs.toString();
    }

    public static void replaceWith(IASTNode node, final String literalString)
    {
        IASTNode copy = (IASTNode)node.clone();
        final Token firstToken = copy.findFirstToken();
        final Token lastToken = copy.findLastToken();
        if (firstToken == null)
            throw new IllegalArgumentException("A node can only be replaced "
                + "with a string if it contains at least one token");
        copy.accept(new GenericASTVisitor()
        {
            @Override public void visitToken(Token token)
            {
                if (token != firstToken) token.setWhiteBefore("");
                token.setText(token == firstToken ? literalString : "");
                if (token != lastToken) token.setWhiteAfter("");
            }
        });
        node.replaceWith(copy);
    }

    public static void print(IASTNode node, PrintStream out)
    {
        for (IASTNode child : node.getChildren())
            child.printOn(out);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utility Classes
    ///////////////////////////////////////////////////////////////////////////

    public static final class NonNullIterator<T> implements Iterator<T>
    {
        private Iterator<T> wrappedIterator;
        private T next;

        public NonNullIterator(Iterator<T> wrappedIterator)
        {
            this.wrappedIterator = wrappedIterator;
            findNext();
        }

        private void findNext()
        {
            do
            {
                if (!this.wrappedIterator.hasNext())
                {
                    this.next = null;
                    return;
                }

                this.next = this.wrappedIterator.next();
            }
            while (this.next == null);
        }

        public boolean hasNext()
        {
            return this.next != null;
        }

        public T next()
        {
            T result = this.next;
            findNext();
            return result;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
