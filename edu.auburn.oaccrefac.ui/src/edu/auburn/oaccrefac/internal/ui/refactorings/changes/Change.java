package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;

public abstract class Change<T extends IASTNode> {

    private T m_node;
    private IProgressMonitor m_pm;
    
    //Internal preprocessor context map
    private Map<IASTNode, List<String> > m_pp_context;
    
    public Change(T nodeToChange) {
        m_node = nodeToChange;
        m_pp_context = new HashMap<>();
    }
    
    public final RefactoringStatus checkConditions(RefactoringStatus init) {
        return this.checkConditions(init, null);
    }
    
    public final RefactoringStatus checkConditions(RefactoringStatus init, IProgressMonitor pm) {
        if (m_pm == null) {
            m_pm = new NullProgressMonitor();
        }
        
        if (m_node == null) {
            init.addFatalError("Change Error: node to be changed cannot be null!");
            return init;
        }
        
        return doCheckConditions(init);
    }
    
    protected DependenceAnalysis performDependenceAnalysis(RefactoringStatus status, 
            IProgressMonitor pm, IASTStatement... statements) {
        try {
            return new DependenceAnalysis(pm, statements);
        } catch (DependenceTestFailure e) {
            status.addError("Dependences could not be analyzed.  " + e.getMessage());
            return null;
        }
    }
    
    /**
     * This method performs the change on the intended node. This 
     * is to be used internally by other changes.
     * @return the changed node.
     * @throws IllegalArgumentException if the node to be changed is frozen
     */
    protected final T change(Change<?> context) {
        //Set this map to context's map
        this.setPreprocessorContext(context.getPreprocessorContext());
        
        if (m_node.isFrozen()) {
            throw new IllegalArgumentException("Error -- changes within changes"
                    + " cannot occur to frozen nodes.");
        }
        return doChange(m_node);
    }
    
    @SuppressWarnings("unchecked")
    public final ASTRewrite change(ASTRewrite rewriter) {
        IASTNode changed = null;
        if (m_node.isFrozen()) {
            changed = doChange((T) m_node.copy());
        } else {
            changed = doChange(m_node);
        }
        //TODO initialize preprocessor map...
        rewriter = rewriter.replace(getOriginal(), changed, null);
        rewriter = replacePreprocessors(rewriter);
        return rewriter;
    }
    
    protected final void replacePreprocessors(IASTNode replace, IASTNode replaceWith) {
        //TODO ... check map and replace preprocessors ...
    }
    
    protected final ASTRewrite replacePreprocessors(ASTRewrite rewriter) {
        //TODO ...do replacements...
        //rewriter = rewriter.insertBefore(...);
        return rewriter;
    }
    
    /**
     * Abstract method describes the implementation that all changes must
     * define. This method takes in a loop and changes it in respect to
     * it's intended purpose.
     * @param loop -- the loop in which to change
     * @return reference to changed loop
     */
    protected abstract T doChange(T nodeToChange);
    
    /**
     * Abstract method for checking the initial conditions for change objects. For
     * example, in this method, it will check the inputs to the constructor to ensure
     * that the inputs are valid.
     * @param init -- Status object that may or may not be changed depending on error
     * @return -- Reference to changed status object
     */
    protected abstract RefactoringStatus doCheckConditions(RefactoringStatus init);
    
    public T getOriginal() { return m_node; }
    public void setProgressMonitor(IProgressMonitor pm) {
        m_pm = pm;
    }
    public IProgressMonitor getProgressMonitor() {
        return m_pm;
    }
    protected void setPreprocessorContext(Map<IASTNode, List<String>> in) {
        if (in != null) {
            m_pp_context = in;
        }
    }
    protected Map<IASTNode, List<String>> getPreprocessorContext() {
        return m_pp_context;
    }
    
}
