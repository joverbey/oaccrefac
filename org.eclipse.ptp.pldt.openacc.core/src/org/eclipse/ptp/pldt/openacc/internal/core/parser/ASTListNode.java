/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Auburn University - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.parser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.ptp.pldt.openacc.internal.core.parser.ASTNodeUtil.NonNullIterator;

@SuppressWarnings("all")
public class ASTListNode<T extends IASTNode> extends ArrayList<T> implements IASTListNode<T>
{
    private IASTNode parent = null;

    ///////////////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////////////

    public ASTListNode()
    {
        super();
    }

    public ASTListNode(int initialCapacity)
    {
        super(initialCapacity);
    }

    public ASTListNode(T singletonElement)
    {
        super(1);
        add(singletonElement);
    }

    public ASTListNode(T... elements)
    {
        super(elements.length);
        for (T e : elements)
            add(e);
    }

    public ASTListNode(Collection<? extends T> copyFrom)
    {
        super(copyFrom);
    }

    public ASTListNode(T first, Collection<? extends T> rest)
    {
        super(rest.size()+1);
        add(first);
        addAll(rest);
    }

    public ASTListNode(Collection<? extends T> firsts, T last)
    {
        super(firsts.size()+1);
        addAll(firsts);
        add(last);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IASTListNode Insertion Methods
    ///////////////////////////////////////////////////////////////////////////

    @Override public void insertBefore(T insertBefore, T newElement)
    {
        int index = indexOf(insertBefore);
        if (index < 0)
            throw new IllegalArgumentException("Element to insert before not in list");
        add(index, newElement);
    }

    @Override public void insertAfter(T insertAfter, T newElement)
    {
        int index = indexOf(insertAfter);
        if (index < 0)
            throw new IllegalArgumentException("Element to insert after not in list");
        add(index+1, newElement);
    }

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
        return this;
    }

    @Override public Iterator<T> iterator()
    {
        // This is a custom iterator that, unlike the usual ArrayList iterator,
        // (1) never returns null and
        // (2) allows the tree to be modified during traversal.
        return new NonNullIterator<T>(new Iterator<T>()
        {
            private int index = 0;

            @Override public boolean hasNext() { return index < size(); }
            @Override public T next() { return index >= size() ? null : get(index++); }
            @Override public void remove() { throw new UnsupportedOperationException(); }
        });
    }

    @Override public void accept(IASTVisitor visitor)
    {
        visitor.visitASTNode(this);
        visitor.visitASTListNode(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Searching
    ///////////////////////////////////////////////////////////////////////////

    @Override public <T extends IASTNode> Set<T> findAll(Class<T> targetClass)
    {
        return ASTNodeUtil.findAll(this, targetClass);
    }

    @SuppressWarnings("hiding")
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
    // Source Manipulation
    ///////////////////////////////////////////////////////////////////////////

    // These methods are all inherited from ArrayList but are overridden to call #setParent
    @Override public T set(int index, T element) { if (element != null) element.setParent(this); return super.set(index, element); }
    @Override public boolean add(T element) { if (element != null) element.setParent(this); return super.add(element); }
    @Override public void add(int index, T element) { if (element != null) element.setParent(this); super.add(index, element); }
    @Override public boolean addAll(Collection<? extends T> c) { for (T element : c) if (element != null) element.setParent(this); return super.addAll(c); }
    @Override public boolean addAll(int index, Collection<? extends T> c) { for (T element : c) if (element != null) element.setParent(this); return super.addAll(index, c); }

    @SuppressWarnings("unchecked")
    @Override public void replaceChild(IASTNode node, IASTNode withNode)
    {
        int i = this.indexOf(node);
        if (i < 0)
            throw new IllegalStateException("Child node not found");
        this.set(i, (T)withNode);
        if (withNode != null) withNode.setParent(this);
        // if (node != null) node.setParent(null);
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

    @SuppressWarnings("unchecked")
    @Override public Object clone()
    {
            ASTListNode<T> copy = new ASTListNode<T>();
            for (IASTNode n : this)
            {
                if (n == null)
                    copy.add(null);
                else
                {
                    T newChild = (T)n.clone();
                    newChild.setParent(copy);
                    copy.add(newChild);
                }
            }
            return copy;
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
