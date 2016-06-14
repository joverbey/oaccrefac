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
package org.eclipse.ptp.pldt.openacc.core.parser;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser.Terminal;

/**
 * Enumerates the terminal symbols in the grammar being parsed
 */
@SuppressWarnings("all")
public class Token implements IASTNode
{
    ///////////////////////////////////////////////////////////////////////////
    // Fields
    ///////////////////////////////////////////////////////////////////////////

    protected IASTNode parent = null;

    /**
     * The type of symbol (i.e., the terminal symbol in the grammar) that this token represents
     */
    protected Terminal terminal = null;

    /**
     * Whitespace and whitetext appearing before this token that should be associated with this token
     */
    protected String whiteBefore = "";

    /**
     * The token text
     */
    protected String text = "";

    /**
     * Whitespace and whitetext appearing after this token that should be associated with this token, not the next
     */
    protected String whiteAfter = "";

    /**
     * This token's offset in the input stream.
     * <p>
     * A token's offset is the (0-based) index of the first character of the token's text ({@link #getText()})
     * in the input stream.
     */
    protected int offset = -1;

    ///////////////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////////////

    public Token(Terminal terminal, String whiteBefore, String text, String whiteAfter, int offset)
    {
        this.terminal    = terminal;
        this.whiteBefore = whiteBefore == null ? "" : whiteBefore;
        this.text        = text        == null ? "" : text;
        this.whiteAfter  = whiteAfter  == null ? "" : whiteAfter;
        this.offset      = offset;
    }

    public Token(Terminal terminal, String whiteBefore, String text, String whiteAfter)
    {
        this(terminal, whiteBefore, text, whiteAfter, -1);
    }

    public Token(Terminal terminal, String text, int offset)
    {
        this(terminal, null, text, null, offset);
    }

    public Token(Terminal terminal, String text)
    {
        this(terminal, text, -1);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Accessor/Mutator Methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return the type of symbol (i.e., the terminal symbol in the grammar) that this token represents
     */
    public Terminal getTerminal() { return terminal; }

    /**
     * Sets the type of symbol (i.e., the terminal symbol in the grammar) that this token represents
     */
    public void setTerminal(Terminal value) { terminal = value; }

    /**
     * @return the token text
     */
    public String getText() { return text; }

    /**
     * Sets the token text
     */
    public void setText(String value) { text = value == null ? "" : value; }

    /**
     * @return whitespace and whitetext appearing before this token that should be associated with this token
     */
    public String getWhiteBefore() { return whiteBefore; }

    /**
     * Sets whitespace and whitetext appearing before this token that should be associated with this token
     */
    public void setWhiteBefore(String value) { whiteBefore = value == null ? "" : value; }

    /**
     * @return whitespace and whitetext appearing after this token that should be associated with this token, not the next
     */
    public String getWhiteAfter() { return whiteAfter; }

    /**
     * Sets whitespace and whitetext appearing after this token that should be associated with this token, not the next
     */
    public void setWhiteAfter(String value) { whiteAfter = value == null ? "" : value; }

    /**
     * Returns this token's offset.
     * <p>
     * A token's offset is the (0-based) index of the first character of the token's text ({@link #getText()})
     * in the input stream.
     *
     * @return the 0-based offset of the first character of this token's text ({@link #getText()})
     */
    public int getOffset() { return offset; }

    /**
     * Sets the 0-based offset of the first character of this token's text ({@link #getText()})
     */
    public void setOffset(int value) { offset = value; }

    /**
     * @return the length of this token's text ({@link #getText()})
     */
    public int getLength() { return getText().length(); }

    ///////////////////////////////////////////////////////////////////////////
    // Debugging Output
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a string describing the token
     */
    @Override public String toString() { return terminal + ": \"" + text + "\""; }

    ///////////////////////////////////////////////////////////////////////////
    // Traversal and Visitor Support
    ///////////////////////////////////////////////////////////////////////////

    @Override public IASTNode getParent()
    {
        return parent;
    }

    @Override public void setParent(IASTNode parent)
    {
        this.parent = parent;
    }

    @Override public Iterable<? extends IASTNode> getChildren()
    {
        return new Iterable<Token>()
        {
        	@Override public Iterator<Token> iterator()
            {
                return new Iterator<Token>()
                {
                    private boolean returned = false;

                    @Override public boolean hasNext()
                    {
                        return returned = false;
                    }

                    @Override public Token next()
                    {
                        if (returned == false)
                        {
                            returned = true;
                            return Token.this;
                        }
                        else return null;
                    }

                    @Override public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override public void accept(IASTVisitor visitor)
    {
        visitor.visitToken(this);
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
            return super.clone();
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
        return this;
    }

    @Override public Token findLastToken()
    {
        return this;
    }

    @Override public boolean isFirstChildInList()
    {
        return ASTNodeUtil.isFirstChildInList(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Source Code Reproduction
    ///////////////////////////////////////////////////////////////////////////

    @Override public void printOn(PrintStream out)
    {
        out.print(whiteBefore);
        out.print(text);
        out.print(whiteAfter);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Equals and HashCode Implementations
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Two <code>Token</code>s are considered equal if their terminal symbols
     * are equal and their text field is equal, regardless of the token's
     * white text or parent node in the syntax tree.
     */
    public boolean isEquivalentTo(Object other)
    {
        if (other == null || !other.getClass().equals(this.getClass())) return false;

        Token o = (Token)other;
        return super.equals(o)
            && o.terminal == this.terminal
            && o.text.equals(this.text);
    }

    @Override public int hashCode()
    {
        int result = 29 + super.hashCode();
        result = 17 * result + terminal.hashCode();
        result = 17 * result + text.hashCode();
        return result;
    }
}
