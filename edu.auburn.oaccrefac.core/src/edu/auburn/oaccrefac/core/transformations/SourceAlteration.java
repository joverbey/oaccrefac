/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Eichelkraut - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.core.transformations;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.Activator;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * This class describes the base class for change objects that use the ASTRewrite in their algorithms.
 * <p>
 * All that the inherited classes need to implement are the two abstract methods {@link #doChange()} and
 * {@link #doCheckConditions(RefactoringStatus)}.
 */
public abstract class SourceAlteration<T extends Check<?>> {
    public static final String PRAGMA = "#pragma";
    public static final String LCURLY = "{";
    public static final String RCURLY = "}";
    public static final String LPARENTH = "(";
    public static final String RPARENTH = ")";
    public static final String SEMICOLON = ";";

    private final IASTRewrite rewriter;
    private final IASTTranslationUnit tu;
    private StringBuilder src;
    private int originalLength;

    /**
     * Offset into the file that the StringBuilder starts at.
     * <p>
     * Should be the affected function definition's offset.
     */
    private int srcOffset;
    protected T check;

    // FIXME should somehow get an IASTRewrite from the tu and only take one argument
    public SourceAlteration(IASTRewrite rewriter, T check) {
        this.tu = check.getTranslationUnit();
        this.rewriter = rewriter;
        this.src = null;
        this.srcOffset = 0;

        if (this.rewriter == null) {
            throw new IllegalArgumentException("Rewriter cannot be null!");
        }
    }

    /**
     * Creates a new change object from an existing one. This allows chaining of changes by ensuring that the new change
     * contains all of the changes made by the previous one.
     * 
     * @param previous
     *            -- the original change
     */
    public SourceAlteration(SourceAlteration<?> previous) {
        this.tu = previous.tu;
        this.rewriter = previous.rewriter;
        this.src = previous.src;
        this.srcOffset = previous.srcOffset;
    }

    // FIXME: Review/fix comments

    /**
     * Abstract method pattern for the inherited class to implement. This method is where the actual rewriting should
     * happen by using the rewriter being sent from the base class on the AST received by the inherited class'
     * constructor.
     */
    protected abstract void doChange() throws Exception;

    // -----------------------------------------------------------------------------------

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

    protected final void insert(int offset, String text) {
        updateAlterationTrackingFields(offset, 0);
        src.insert(offset - srcOffset, text);
    }

    protected final void insertAfter(IASTNode node, String text) {
        insert(node.getFileLocation().getNodeOffset() + node.getFileLocation().getNodeLength(), text);
    }

    protected final void remove(int offset, int length) {
        updateAlterationTrackingFields(offset, length);
        src.delete(offset - srcOffset, offset - srcOffset + length);
    }

    protected final void remove(IASTNode node) {
        remove(node.getFileLocation().getNodeOffset(), node.getFileLocation().getNodeLength());
    }

    protected final void replace(int offset, int length, String text) {
        updateAlterationTrackingFields(offset, length);
        src.replace(offset - srcOffset, offset - srcOffset + length, text);
    }

    protected final void replace(IASTNode node, String text) {
        replace(node.getFileLocation().getNodeOffset(), node.getFileLocation().getNodeLength(), text);
    }

    // FIXME -- Unused
    protected final String getCurrentTextAt(int offset, int length) {
        return src.substring(offset - srcOffset, offset - srcOffset + length);
    }

    protected final String getCurrentText() {
        return src.toString();
    }

    /**
     * Allows inherited classes to get any pragmas associated with a node. As of now, only {@link IASTForStatement}
     * nodes are supported
     * 
     * @param node
     *            -- node to retrieve pragmas from
     * @return -- array of {@link String} representing literal pragma text
     * @throws UnsupportedOperationException
     *             if node is not {@link IASTForStatement}
     */
    // FIXME - Unused; delete or move to Inquisitor?
    protected final String[] getPragmaStrings(IASTForStatement node) {
        List<IASTPreprocessorPragmaStatement> p = InquisitorFactory.getInquisitor(node).getLeadingPragmas();
        String[] pragCode = new String[p.size()];
        for (int i = 0; i < pragCode.length; i++) {
            pragCode[i] = p.get(i).getRawSignature();
        }
        return pragCode;
    }

    protected final List<IASTPreprocessorPragmaStatement> getPragmas(IASTForStatement node) {
        return InquisitorFactory.getInquisitor(node).getLeadingPragmas();
    }

    protected final String pragma(String code) {
        return PRAGMA + " " + code.trim() + System.lineSeparator();
    }

    protected final String compound(String code) {
        return LCURLY + 
                //(code.startsWith(System.lineSeparator()) ? "" : System.lineSeparator()) + 
                code.trim() + 
                //(code.endsWith(System.lineSeparator()) ? "" : System.lineSeparator()) + 
                RCURLY;
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
        return LPARENTH + code + RPARENTH;
    }

    private void updateAlterationTrackingFields(int offset, int length) {
        if(src == null) {
            originalLength = length;
        }
        else {
            //edit area overlaps start of src
            if(offset < srcOffset && srcOffset < offset + length) {
                originalLength = originalLength + length - (offset + length - srcOffset); 
            }
            //edit area overlaps end of src
            else if(offset < srcOffset + originalLength && srcOffset + originalLength < offset + length) {
                originalLength = originalLength + length - (srcOffset + originalLength - offset);
            }
            //edit area is entirely before src
            else if(offset + length <= srcOffset) {
                originalLength = originalLength + length + (srcOffset - (offset + length));
            }
            //edit area is entirely after src
            else if(srcOffset + originalLength <= offset) {
                originalLength = originalLength + length + (offset - (srcOffset + originalLength));
            }
            //otherwise, edit area is contained in src, so no update to originalLength are needed
        }
        
        if (src == null) {
            src = new StringBuilder(tu.getRawSignature().substring(offset, offset + length));
            srcOffset = offset;
        }
        else {
            if(offset < srcOffset) {
                src.insert(0, tu.getRawSignature().substring(offset, srcOffset));
                srcOffset = offset;
            }
            if(offset + length > srcOffset + src.length()) {
                src.append(tu.getRawSignature().substring(srcOffset + src.length(), offset));
            }
        }
        
    }
    
    /**
     * Passes all "cached" changes to the rewriter. Must be called after all changes are made to cause changes to
     * actually occur
     */
    public final void finalizeChanges() {
        TextEditGroup teg = new TextEditGroup("teg");
        teg.addTextEdit(new ReplaceEdit(srcOffset, originalLength, ASTUtil.format(getCurrentText())));
        rewriter.insertBefore(tu, tu.getChildren()[0], rewriter.createLiteralNode(""), teg);
    }

    /**
     * Called by the CLI to rewrite the AST. Not used by the Eclipse GUI.
     * 
     * @return
     */
    public Change rewriteAST() {
        return rewriter.rewriteAST();
    }
    
}
