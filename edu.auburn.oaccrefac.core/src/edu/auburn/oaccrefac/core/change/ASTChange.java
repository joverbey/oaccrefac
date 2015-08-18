package edu.auburn.oaccrefac.core.change;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.dependence.check.DependenceCheck;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * This class describes the base class for change objects that
 * use the ASTRewrite in their algorithms. 
 * 
 * All that the inherited classes need to implement are the two 
 * abstract methods {@link #doChange()} and 
 * {@link #doCheckConditions(RefactoringStatus)}.
 * @author Adam Eichelkraut
 *
 */

public abstract class ASTChange {
    
    //Members
    private final IASTRewrite m_rewriter;
    private final IASTTranslationUnit m_tu;
    private StringBuilder m_src;
    private int originalLength;
    //offset into the file that the StringBuilder starts at - 
    //should be the affected function definition's offset
    private int srcOffset;
    
    public static final String PRAGMA = "#pragma ";
    public static final String COMP_OPEN = " { ";
    public static final String COMP_CLOSE = " } ";
    
    //FIXME should somehow get an IASTRewrite from the tu and only take one argument
    public ASTChange(IASTTranslationUnit tu, IASTRewrite rewriter) {
        this.m_tu = tu;
        this.m_rewriter = rewriter;
        this.m_src = null;
        this.srcOffset = 0;
        
        if (m_rewriter == null) {
            throw new IllegalArgumentException("Rewriter cannot be null!");
        }
        
    }
    
    /**
     * Creates a new change object from an existing one.
     * This allows chaining of changes by ensuring that the new change
     * contains all of the changes made by the previous one. 
     * @param previous -- the original change
     */
    public ASTChange(ASTChange previous) {
        this.m_tu = previous.m_tu;
        this.m_rewriter = previous.m_rewriter;
        this.m_src = previous.m_src;
        this.srcOffset = previous.srcOffset;
    }
    
    /**
     * Abstract method pattern for the inherited class to implement. This method is
     * where the actual rewriting should happen by using the rewriter being sent from
     * the base class on the AST received by the inherited class' constructor.
     * @author Adam Eichelkraut
     */
    protected abstract void doChange();
    
    /**
     * Abstract method pattern for inherited class to implement. This method is
     * where the checking of preconditions for the inherited class should happen.
     * For example, if there were any inputs or parameters to the change, they should
     * be checked to make sure they make sense and are valid. Dependency checking
     * should also happen in this by creating an instance of some {@link DependenceCheck}
     * class.
     * @author Adam Eichelkraut
     * @param init -- status to change
     * @param pm -- the progress monitor, if needed
     */
    protected abstract void doCheckConditions(RefactoringStatus init, IProgressMonitor pm);
    //-----------------------------------------------------------------------------------
    
    /**
     * Should always be called before changes are made. Checks all preconditions
     * for the inherited change object. This method calls the abstract 
     * {@link #doCheckConditions(RefactoringStatus)} to be defined in the inherited class.
     * @author Adam Eichelkraut
     * @param init -- status to be changed
     * @param pm -- progress monitor, can be null if not needed
     * @return -- possibly changed status
     */
    public final RefactoringStatus checkConditions(RefactoringStatus init, IProgressMonitor pm) {
//        if (m_pm == null) {
//            m_pm = new NullProgressMonitor();
//        }
        doCheckConditions(init, pm);
        return init;
    }
    
    /**
     * Base change method for all inherited classes. This method does some
     * initialization before calling the inherited class' implemented
     * {@link #doChange(IASTRewrite)} method.
     * @author Adam Eichelkraut
     * @return -- returns the top-level rewriter used
     * @throws IllegalArgumentException if the rewriter is null at this point
     */
    public final void change() {
        doChange();
    }

    protected final void insert(int offset, String text) {
        if(m_src == null) {
            initializeStringBuilder(offset);
        }
        
        m_src.insert(offset - srcOffset, text);
    }
    
    protected final void remove(int offset, int length) {
        if(m_src == null) {
            initializeStringBuilder(offset);
        }

        m_src.delete(offset - srcOffset, offset - srcOffset + length);
    }
    
    protected final void remove(IASTNode node) {
        remove(node.getFileLocation().getNodeOffset(),
               node.getFileLocation().getNodeLength());
    }
    
    protected final void replace(int offset, int length, String text) {
        if(m_src == null) {
            initializeStringBuilder(offset);
        }
        
        m_src.replace(offset - srcOffset, offset - srcOffset + length, text);
    }
    
    protected final void replace(IASTNode node, String text) {
        replace(node.getFileLocation().getNodeOffset(),
                node.getFileLocation().getNodeLength(),
                text);
    }
    
    protected final String getCurrentTextAt(int offset, int length) {
        return m_src.substring(offset - srcOffset, offset - srcOffset + length);
    }
    
    protected final String getText() {
        return m_src.toString();
    }
    

    /**
     * Allows inherited classes to get any pragmas associated with a node.
     * As of now, only {@link IASTForStatement} nodes are supported
     * @param node -- node to retrieve pragmas from
     * @return -- array of {@link String} representing literal pragma text
     * @throws UnsupportedOperationException if node is not {@link IASTForStatement}
     */
    protected final String[] getPragmaStrings(IASTForStatement node) {
        List<IASTPreprocessorPragmaStatement> p = InquisitorFactory.getInquisitor(node).getLeadingPragmas();
        String[] pragCode = new String[p.size()];
        for(int i = 0; i < pragCode.length; i++) {
            pragCode[i] = p.get(i).getRawSignature();
        }
        return pragCode; 
    }

    protected final List<IASTPreprocessorPragmaStatement> getPragmas(IASTForStatement node) {
        return InquisitorFactory.getInquisitor(node).getLeadingPragmas();
    }
    
    protected final String pragma(String code) {
        return PRAGMA + code + System.lineSeparator();
    }
    
    protected final String compound(String code) {
        return COMP_OPEN + code + COMP_CLOSE;
    }
    
    protected final String forLoop(String init, String cond, String iter, String body) {
        if (!init.trim().endsWith(";"))
            init += ";";
        if (!cond.trim().endsWith(";"))
            cond += ";";
        StringBuilder sb = new StringBuilder(String.format("for (%s %s %s)", init, cond, iter));
        sb.append(System.lineSeparator());
        sb.append(body);
        return sb.toString();
    }
    
    private void initializeStringBuilder(int offset) {
        for(IASTFunctionDefinition func : ASTUtil.find(m_tu, IASTFunctionDefinition.class)) {
            if(offset >= func.getFileLocation().getNodeOffset()
                    && offset <= func.getFileLocation().getNodeOffset() + func.getFileLocation().getNodeLength()) {
                m_src = new StringBuilder(func.getRawSignature());
                srcOffset = func.getFileLocation().getNodeOffset();
                originalLength = func.getFileLocation().getNodeLength();
                return;
            }
        }
        throw new StringIndexOutOfBoundsException();
    }
    
    /**
     * Passes all "cached" changes to the rewriter. Must be called 
     * after all changes are made to cause changes to actually occur
     */
    public final void finalizeChanges() {
        TextEditGroup teg = new TextEditGroup("teg");
        teg.addTextEdit(new ReplaceEdit(srcOffset, originalLength, ASTUtil.format(getText())));
        m_rewriter.insertBefore(m_tu, m_tu.getChildren()[0], m_rewriter.createLiteralNode(""), teg);
    }

    protected IASTTranslationUnit getTranslationUnit() {
        return m_tu;
    }
    
//    private boolean sourceSubstringIsWithinCorrectBounds(int offset, int length) {
//        return offset >= srcOffset && offset <= srcOffset + src.length() 
//            && offset + length >= srcOffset && offset + length <= srcOffset + length 
//            && length > 0;
//    }
    
    /**
     * Called by the CLI to rewrite the AST.  Not used by the Eclipse GUI.
     * @return
     */
    public Change rewriteAST() {
        return m_rewriter.rewriteAST();
    }
}
