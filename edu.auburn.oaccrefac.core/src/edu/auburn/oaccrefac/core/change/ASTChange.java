package edu.auburn.oaccrefac.core.change;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.dependence.check.DependenceCheck;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * This class describes the base class for change objects that
 * use the ASTRewrite in their algorithms. This class also attempts
 * to keep track of associated pragmas via the 'safe' methods
 * found in this class.
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
    private TextEditGroup m_teg;
    private Map<IASTNode, List<IASTPreprocessorPragmaStatement>> m_pp_map;
    
    /**
     * Constructor -- initializes pragma map
     * @author Adam Eichelkraut
     * @param rewriter: base rewriter to use for inherited implementations
     */
    public ASTChange(IASTRewrite rewriter) {
        m_rewriter = rewriter;
        m_teg = new TextEditGroup("edits");
        m_pp_map = new HashMap<IASTNode, List<IASTPreprocessorPragmaStatement>>();
    }
    
    //ABSTRACT METHODS ------------------------------------------------------------------
    
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
     * Abstract method pattern for inherted class to implement. This method is
     * where the checking of preconditions for the inherted class should happen.
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
        return m_rewriter;
        
    }
    
    /**
     * This method is to be used in the inherited implementations of 
     * {@link #doChange(IASTRewrite)} to replace a node in the AST with
     * a replacement. The method returns a rewriter for further rewriting
     * the replaced node. See ASTRewrite_README.txt in this package directory
     * for more info on rewriters.
     * 
     * FIXME pragmas are to be inserted before the replacement if needed
     * 
     * @param rewriter -- the rewriter to use for the replacement
     * @param node -- the node to replace
     * @param replacement -- the new node
     * @return -- a rewriter for further rewriting the replaced node
     */
    protected IASTRewrite safeReplace(IASTRewrite rewriter, 
            IASTNode node, IASTNode replacement) {
        if(replacement != null && m_pp_map.containsKey(replacement)) {
            for(IASTPreprocessorPragmaStatement prag : m_pp_map.get(replacement)) {
                rewriter.insertBefore(node.getParent(), node, rewriter.createLiteralNode(prag.getRawSignature() + System.lineSeparator()), null);
            }
        }
        return rewriter.replace(node, replacement, null);
    }
    
    /**
     * This method is to be used in the inherited implementations of 
     * {@link #doChange(IASTRewrite)} to insert a new node in the AST.
     * See ASTRewrite_README.txt in this package directory for more info 
     * on rewriters.
     * 
     * FIXME pragmas are supposed to be inserted before the newly inserted node
     * 
     * @param rewriter -- rewriter to use for insertion
     * @param parent -- parent of 'insertionPoint'
     * @param insertionPoint -- node location in which to insert before
     * @param newNode -- new node to be inserted
     * @return -- new rewriter for further rewriting inserted 'newNode'
     */
    protected IASTRewrite safeInsertBefore(IASTRewrite rewriter,
            IASTNode parent, IASTNode insertionPoint, IASTNode newNode) {
        if(newNode != null && m_pp_map.containsKey(newNode)) {
            for(IASTPreprocessorPragmaStatement prag : m_pp_map.get(newNode)) {
                rewriter.insertBefore(parent, insertionPoint, rewriter.createLiteralNode(prag.getRawSignature() + System.lineSeparator()), null);
            }
        }
        return rewriter.insertBefore(parent, insertionPoint, newNode, null);
    }
    
    /**
     * Safely removes the node from using the rewriter
     * @param rewriter -- rewriter to remove node with
     * @param node -- node to be removed
     */
    protected void safeRemove(IASTRewrite rewriter, IASTNode node) {
        rewriter.remove(node, null);
    }
    
    /**
     * This method is a way to copy any {@link IASTNode}. Inherited classes
     * should use this in their implementation of {@link #doChange(IASTRewrite)}
     * in order to ensure that any associated pragmas with the original node
     * are maintained and reinserted afterwards.
     * 
     * As of now, only {@link IASTForStatement} nodes are supported 
     * for maintaining pragmas.
     * 
     * @param node -- node to be copied
     * @return -- fresh, unfrozen copy of node not apart of the node's 
     *            AST and with no parent
     */
    @SuppressWarnings("unchecked")
    protected <T extends IASTNode> T safeCopy(T node) {
        T copy = (T) node.copy(CopyStyle.withLocations);
        if(node instanceof IASTForStatement) {
            m_pp_map.put(copy, InquisitorFactory.getInquisitor((IASTForStatement) node).getLeadingPragmas());
        }
        return copy;
    }
    
    /**
     * Allows inherited classes to get any pragmas associated with a node.
     * As of now, only {@link IASTForStatement} nodes are supported
     * @param node -- node to retrieve pragmas from
     * @return -- array of {@link String} representing literal pragma text
     * @throws UnsupportedOperationException if node is not {@link IASTForStatement}
     */
    protected String[] getPragmas(IASTNode node) {
        if(!(node instanceof IASTForStatement)) {
            throw new UnsupportedOperationException("Currently only support pragmas on for loops");
        }
        List<IASTPreprocessorPragmaStatement> p = InquisitorFactory.getInquisitor((IASTForStatement) node).getLeadingPragmas();
        String[] pragCode = new String[p.size()];
        for(int i = 0; i < pragCode.length; i++) {
            pragCode[i] = p.get(i).getRawSignature();
        }
        return pragCode; 
    }
    
    /**
     * Allows inherited classes to insert list of pragmas before an {@link IASTNode}
     * As of right now, only {@link IASTForStatement} nodes are supported
     * @param node -- node to insert pragmas before
     * @param pragmas -- arbitrary list of pragmas as {@link String} objects
     * @throws UnsupportedOperationException if node is not {@link IASTForStatement}
     */
    protected void insertPragmas(IASTNode node, String... pragmas) {
        if(!(node instanceof IASTForStatement)) {
            throw new UnsupportedOperationException("Currently only support pragmas on for loops");
        }
        for(String pragma : pragmas) {
            if(!pragma.startsWith("#pragma")) {
                throw new IllegalArgumentException("String is not a pragma");
            }
            InsertEdit ie = new InsertEdit(node.getFileLocation().getNodeOffset(), pragma + System.lineSeparator());
            m_teg.addTextEdit(ie);
        }
    }
    
    /**
     * Removes all pragmas from a given {@link IASTNode} by traversing the node tree and
     * creating a {@link DeleteEdit} object for each 
     * {@link IASTPreprocessorPragmaStatement} found.
     * As of right now, only {@link IASTForStatement} nodes have pragmas removed
     * 
     * @param node -- node tree to remove pragmas from
     */
    protected void deepRemovePragmas(IASTNode node) {
        class PragmaRemover extends ASTVisitor {
            
            public PragmaRemover() { 
                shouldVisitStatements = true;
            }
            
            @Override
            public int visit(IASTStatement stmt) {
                if(stmt instanceof IASTForStatement) {
                    for(IASTPreprocessorPragmaStatement prag : InquisitorFactory.getInquisitor((IASTForStatement) stmt).getLeadingPragmas()) {
                        DeleteEdit de = new DeleteEdit(prag.getFileLocation().getNodeOffset(), prag.getFileLocation().getNodeLength() + 1);
                        m_teg.addTextEdit(de);
                    }
                }
                return PROCESS_CONTINUE;
            }
        }
        node.accept(new PragmaRemover());
    }
    
    /**
     * Removes pragmas from a specified {@link IASTNode} by creating
     * {@link DeleteEdit} objects for the specific file location of the node's pragma
     * As of right now, only {@link IASTForStatement} nodes are supported.
     * @param node
     * @throws UnsupportedOperationException if node is not {@link IASTForStatement}
     */
    protected void removePragmas(IASTNode node) {
        if(!(node instanceof IASTForStatement)) {
            throw new UnsupportedOperationException("Currently only support pragmas on for loops");
        }
        for(IASTPreprocessorPragmaStatement prag : InquisitorFactory.getInquisitor((IASTForStatement) node).getLeadingPragmas()) {
            DeleteEdit de = new DeleteEdit(prag.getFileLocation().getNodeOffset(), prag.getFileLocation().getNodeLength() + 1);
            m_teg.addTextEdit(de);
        }
    }
    
    /**
     * Takes a translation unit, and using a text edit group, adds all text edits created
     * from the insert, deepRemove, or remove methods.
     * @param tu -- translation in which to add a blank literal node at beginning of
     */
    protected void finalizePragmas(IASTTranslationUnit tu) {
        m_rewriter.insertBefore(tu, tu.getChildren()[0], m_rewriter.createLiteralNode(""), m_teg);
    }
    
    /**
     * Sets the rewriter object for this class
     * @param rewriter -- rewriter to set
     */
    public void setRewriter(IASTRewrite rewriter) {
        m_rewriter = rewriter;
    }
    
    /**
     * Gets the rewriter object for this object
     * @return -- the set rewriter
     */
    public IASTRewrite getRewriter() { return m_rewriter; }
    
    /**
     * Sets the progress monitor for the operations in this class.
     * @param pm -- progress monitor to set
     */
    public void setProgressMonitor(IProgressMonitor pm) {
        m_pm = pm;
    }
    
    /**
     * Gets the progress monitor set for this object
     * @return -- progress monitor
     */
    public IProgressMonitor getProgressMonitor() { return m_pm; }
    
}
