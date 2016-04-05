/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Eichelkraut - initial API and implementation
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.Activator;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * SourceAlteration describes the base class for change objects that use the
 * ASTRewrite in their algorithms.
 * <p>
 * All that the inheriting classes need to implement are two abstract methods 
 * {@link #doChange()} and {@link #doCheckConditions(RefactoringStatus)}.
 * 
 * @author Adam Eichelkraut
 */
public abstract class SourceAlteration<T extends Check<?>> {
    public static final String PRAGMA = "#pragma";
    public static final String LCURLY = "{";
    public static final String RCURLY = "}";
    public static final String LPAREN = "(";
    public static final String RPAREN = ")";
    public static final String SEMICOLON = ";";
    public static final String COMMA = ",";
    public static final String NL = System.lineSeparator();
    public static final String COPYIN = "copyin";
    public static final String COPYOUT = "copyout";

    private final IASTRewrite rewriter;
    private final IASTTranslationUnit tu;
    
    protected T check;
    
    //string to insert in the final replacement
    private StringBuilder src;
    //length of the section to replace 
    private int endOffset;
    //offset into the file to perform the final replacement
    private int startOffset;
    
    private List<Repl> repls;
    private int numRepls;
    
    // FIXME should somehow get an IASTRewrite from the tu and only take one argument
    public SourceAlteration(IASTRewrite rewriter, T check) {
        this.tu = check.getTranslationUnit();
        this.rewriter = rewriter;
        this.src = null;
        this.startOffset = -1;
        this.endOffset = -1;
        this.check = check;
        this.repls = new ArrayList<Repl>();
        this.numRepls = 0;

        if (this.rewriter == null) {
            throw new IllegalArgumentException("Rewriter cannot be null!");
        }
    }

    /**
     * Abstract method pattern for the inherited class to implement. This method is where the actual rewriting should
     * happen by using the rewriter being sent from the base class on the AST received by the inherited class'
     * constructor.
     */
    protected abstract void doChange() throws Exception;

    /**
     * Base change method for all inherited classes. This method does some initialization before calling the inherited
     * class' implemented {@link #doChange(IASTRewrite)} method.
     * 
     * @return -- returns the top-level rewriter used
     * 
     * @throws IllegalArgumentException
     *             if the rewriter is null at this point
     */
    public final void change() throws CoreException {
        // FIXME: Eliminate doChange -- subclasses can override change() instead???
        try {
            doChange();
        } catch (Exception e) {
            Activator.log(e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
    }

    /**
     * perform a text insertion
     * 
     * @param offset the offset in the file to insert the text at
     * @param text the text to insert
     * @return false if the change overlaps a previous one; true otherwise
     */
    protected final boolean insert(int offset, String text) {
        return replace(offset, 0, text);
    }

    /**
     * perform a text insertion after the given node
     * 
     * @param node the node after which to place the text
     * @param text the text to insert
     * @return false if the change overlaps a previous one; true otherwise
     */
    protected final boolean insertAfter(IASTNode node, String text) {
        return insert(node.getFileLocation().getNodeOffset() + node.getFileLocation().getNodeLength(), text);
    }
    
    /**
     * perform a text insertion before a given node
     * 
     * @param node the node before which to insert the text
     * @param text the text to insert
     * @return false if the change overlaps a previous one; true otherwise
     */
    protected final boolean insertBefore(IASTNode node, String text) {
        return insert(node.getFileLocation().getNodeOffset(), text);
    }

    /**
     * perform a text removal
     * 
     * @param offset the starting offset of the text to remove
     * @param length the length of the text to remove
     * @return false if the change overlaps a previous one; true otherwise
     */
    protected final boolean remove(int offset, int length) {
       return replace(offset, length, "");
    }

    /**
     * perform a text removal
     * 
     * @param node the node to remove
     * @return false if the change overlaps a previous one; true otherwise
     */
    protected final boolean remove(IASTNode node) {
        return remove(node.getFileLocation().getNodeOffset(), node.getFileLocation().getNodeLength());
    }

    /**
     * perform a text replacement
     * 
     * @param offset the starting offset of the text to replace
     * @param length the length of the text to replace
     * @param text the text to replace the given range with
     * @return false if the change overlaps a previous one; true otherwise
     */
    protected final boolean replace(int offset, int length, String text) {
        updateAlterationTrackingFields(offset, offset + length);
        repls.add(new Repl(offset, offset + length, text, numRepls++));
        return combineOverlappingRepls();
    }

    /**
     * perform a text removal
     * 
     * @param node the node to replace
     * @param length the text to replace the node with
     * @return false if the change overlaps a previous one; true otherwise
     */
    protected final boolean replace(IASTNode node, String text) {
        return replace(node.getFileLocation().getNodeOffset(), node.getFileLocation().getNodeLength(), text);
    }

    protected final String pragma(String code) {
        return " " + PRAGMA + " " + code.trim();
    }
    
    protected final String copyin(String... vars) {
        StringBuilder sb = new StringBuilder(COPYIN + LPAREN);
        String separator = "";
        for(String var : vars) {
            sb.append(separator);
            sb.append(var.trim());
            separator = COMMA;
        }
        sb.append(RPAREN);
        return sb.toString();
    }
    
    protected final String copyout(String... vars) {
        StringBuilder sb = new StringBuilder(COPYOUT + LPAREN);
        String separator = "";
        for(String var : vars) {
            sb.append(separator);
            sb.append(var.trim());
            separator = COMMA;
        }
        sb.append(RPAREN);
        return sb.toString();
    }

    protected final String compound(String code) {
        return LCURLY + code.trim() + RCURLY;
    }

    protected final String decompound(String code) {
        if (code.trim().startsWith(LCURLY.trim()) && code.trim().endsWith(RCURLY.trim())) {
            return code.trim().substring(1, code.trim().length() - 1).trim();
        } else {
            return code;
        }
    }

    protected final String forLoop(String init, String cond, String iter, String body) {
        if (!init.trim().endsWith(SEMICOLON))
            init += SEMICOLON;
        if (!cond.trim().endsWith(SEMICOLON))
            cond += SEMICOLON;
        StringBuilder sb = new StringBuilder(String.format("for (%s %s %s)", init, cond, iter));
        sb.append(System.lineSeparator());
        sb.append(body);
        return sb.toString();
    }
    
    protected final String forLoop(IASTStatement init, IASTExpression cond, IASTExpression iter, String body) {
        return forLoop(init.getRawSignature(), cond.getRawSignature(), iter.getRawSignature(), body);
    }
    
    protected final String parenth(String code) {
        return LPAREN + code + RPAREN;
    }

    private boolean combineOverlappingRepls() {
        boolean foundOverlapping = true;
        
        repls.sort(new Comparator<Repl>() {

            @Override
            public int compare(SourceAlteration<T>.Repl o1, SourceAlteration<T>.Repl o2) {
                return o1.which - o2.which;
            }
            
        });
        for(int i = 0; i < repls.size(); i++) {
            int j = i + 1;
            while(j < repls.size()) {
                Repl a = repls.get(i);
                Repl b = repls.get(j);
                String text = null;
                //if the replacements overlap
                if((a.startOffset == b.startOffset) 
                        || (b.startOffset <= a.startOffset && a.startOffset < b.endOffset) 
                        || (a.startOffset <= b.startOffset && b.startOffset < a.endOffset)) {
                    //a contains b
                    if(a.startOffset < b.startOffset && a.endOffset > b.endOffset) {
                        text = b.text + a.text;
                    }
                    //b contains a
                    else if(b.startOffset < a.startOffset && b.endOffset > a.endOffset) {
                        text = a.text + b.text;
                    }
                    //a moves into b
                    else if(a.startOffset < b.startOffset && b.startOffset < a.endOffset && a.endOffset < b.endOffset) {
                        text = a.text + b.text;
                    }
                    //b moves into a
                    else if(b.startOffset < a.startOffset && a.startOffset < b.endOffset && b.endOffset < a.endOffset) {
                        text = b.text + a.text;
                    }
                    //same start
                    else if(a.startOffset == b.startOffset) {
                        if(a.which < b.which) {
                            text = b.text + a.text;
                        }
                        else {
                            text = a.text + b.text;
                        }
                    }
                    //same end
                    else if(a.endOffset == b.endOffset) {
                        if(a.which < b.which) {
                            text = a.text + b.text;
                        }
                        else {
                            text = b.text + a.text;
                        }
                    }
                    repls.remove(a);
                    repls.remove(b);
                    repls.add(new Repl(Math.min(a.startOffset, b.startOffset), Math.max(a.endOffset, b.endOffset), text, numRepls++));
                    foundOverlapping = false;
                }
                //non-overlapping
                else {
                    j++;
                }
            }
        }
        return foundOverlapping;
    }
    
    private void updateAlterationTrackingFields(int startOffset, int endOffset) {
        if(src != null) {
            
            //if the new change extends before what is being tracked, add the text up until the beginning of the new change
            if(startOffset < this.startOffset) {
                src.insert(0, tu.getRawSignature().substring(startOffset, this.startOffset));
                this.startOffset = startOffset;
            }
            //if the new change extends after what is being tracked, add the text up until the end of the new change
            if(endOffset > this.endOffset) {
                src.append(tu.getRawSignature().substring(this.endOffset, endOffset));
                this.endOffset = endOffset;
            }
        }
        else {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            src = new StringBuilder(tu.getRawSignature().substring(startOffset, endOffset));
        }
    }
    
    public final void finalizeChanges() {
        if (src != null) {
            int adjust = 0;
            repls.sort(new Comparator<Repl>() {

                @Override
                public int compare(SourceAlteration<T>.Repl o1, SourceAlteration<T>.Repl o2) {
                    return o1.startOffset - o2.startOffset;
                }
                
            });
            for(Repl repl : repls) {
                src.replace(repl.startOffset - this.startOffset + adjust, repl.endOffset - this.startOffset + adjust, repl.text);
                adjust += repl.text.length() - (repl.endOffset - repl.startOffset);
            }
            
            TextEditGroup teg = new TextEditGroup("teg");
            teg.addTextEdit(new ReplaceEdit(this.startOffset, this.endOffset - this.startOffset, ASTUtil.format(src.toString())));
            rewriter.insertBefore(tu, tu.getChildren()[0], rewriter.createLiteralNode(""), teg);
        }
    }
    
    public IASTTranslationUnit getTranslationUnit() {
        return tu;
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + repls.toString();
    }

    /**
     * Called by the CLI to rewrite the AST. Not used by the Eclipse GUI.
     * 
     * @return
     */
    public Change rewriteAST() {
        return rewriter.rewriteAST();
    }
 
    private class Repl {
        int startOffset;
        int endOffset;
        String text;
        int which;
        
        Repl(int startOffset, int endOffset, String text, int which) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.text = text;
            this.which = which;
        }
        
        @Override
        public String toString() {
            return "[" + startOffset + ", " + endOffset + ") --> \"" + text + "\"";
        }
    }
    
}
