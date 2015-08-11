package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.dependence.check.DependenceCheck;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * This class describes the base class for change objects that
 * use the ASTRewrite in their algorithms. 
 * 
 * All that the inherited classes need to implement are the two 
 * abstract methods {@link #doChange(IASTRewrite)} and 
 * {@link #doCheckConditions(RefactoringStatus)}.
 * @author Adam Eichelkraut
 *
 */
public abstract class ASTChange {
    
    //Members
    private IASTRewrite m_rewriter;
    private IProgressMonitor m_pm;
    private IASTTranslationUnit m_tu;
    private StringBuilder m_src;

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
    }
    
    /**
     * Abstract method pattern for the inherited class to implement. This method is
     * where the actual rewriting should happen by using the rewriter being sent from
     * the base class on the AST received by the inherited class' constructor.
     * @author Adam Eichelkraut
     * @param rewriter -- rewriter to be sent to implementation
     * @return -- the original rewriter? I'm not sure this is needed.
     */
    protected abstract IASTRewrite doChange(IASTRewrite rewriter);
    
    /**
     * Abstract method pattern for inherited class to implement. This method is
     * where the checking of preconditions for the inherited class should happen.
     * For example, if there were any inputs or parameters to the change, they should
     * be checked to make sure they make sense and are valid. Dependency checking
     * should also happen in this by creating an instance of some {@link DependenceCheck}
     * class.
     * @author Adam Eichelkraut
     * @param init -- status to change
     * @return -- possibly changed status
     */
    protected abstract RefactoringStatus doCheckConditions(RefactoringStatus init);
    //-----------------------------------------------------------------------------------
    
    /**
     * Should be called before performing 'change' method. Checks all preconditions
     * for the inherited change object. This method calls the abstract 
     * {@link #doCheckConditions(RefactoringStatus)} to be defined in the inherited class.
     * @author Adam Eichelkraut
     * @param init -- status to be changed
     * @param pm -- progress monitor, can be null if not needed
     * @return -- possibly changed status
     */
    public final RefactoringStatus checkConditions(RefactoringStatus init, IProgressMonitor pm) {
        if (m_pm == null) {
            m_pm = new NullProgressMonitor();
        }
        
        return doCheckConditions(init);
    }
    
    /**
     * Base change method for all inherited classes. This method does some
     * initialization before calling the inherited class' implemented
     * {@link #doChange(IASTRewrite)} method.
     * @author Adam Eichelkraut
     * @return -- returns the top-level rewriter used
     * @throws IllegalArgumentException if the rewriter is null at this point
     */
    public final IASTRewrite change() {
        if (m_rewriter == null) {
            throw new IllegalArgumentException("Rewriter cannot be null!");
        }
        
        doChange(m_rewriter);
        
        TextEditGroup teg = new TextEditGroup("teg");
        teg.addTextEdit(new ReplaceEdit(srcOffset, m_src.length(), m_src.toString()));
        m_rewriter.insertBefore(m_tu, m_tu.getChildren()[0], m_rewriter.createLiteralNode(""), new TextEditGroup("teg"));
        return m_rewriter;
        
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
    
    protected final void replace(int offset, int length, String text) {
        if(m_src == null) {
            initializeStringBuilder(offset);
        }
        
        m_src.replace(offset - srcOffset, offset - srcOffset, text);
    }
    
    protected String getCurrentTextAt(int offset, int length) {
        return m_src.substring(offset - srcOffset, offset - srcOffset + length);
    }
    
    protected String pragma(String code) {
        return PRAGMA + code;
    }
    
    protected String compound(String code) {
        return COMP_OPEN + code + COMP_CLOSE;
    }
    
    public IProgressMonitor getProgressMonitor() {
        return m_pm;
    }

    public void setProgressMonitor(IProgressMonitor pm) {
        this.m_pm = pm;
    }
    
    private void initializeStringBuilder(int offset) {
        if(offset < 0) throw new StringIndexOutOfBoundsException();
        
        for(IASTFunctionDefinition func : ASTUtil.find(m_tu, IASTFunctionDefinition.class)) {
            if(offset >= func.getFileLocation().getNodeOffset()
                    && offset <= func.getFileLocation().getNodeOffset() + func.getFileLocation().getNodeLength()) {
                m_src = new StringBuilder(func.getRawSignature());
                break;
            }
        }
    }
    
//    private boolean sourceSubstringIsWithinCorrectBounds(int offset, int length) {
//        return offset >= srcOffset && offset <= srcOffset + src.length() 
//            && offset + length >= srcOffset && offset + length <= srcOffset + length 
//            && length > 0;
//    }
    
    
    
}
