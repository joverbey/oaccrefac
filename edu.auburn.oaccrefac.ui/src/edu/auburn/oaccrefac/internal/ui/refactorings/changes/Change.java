package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;

public abstract class Change<T extends IASTNode> {

    private T m_node;
    private IProgressMonitor m_pm;
    
    public Change(T nodeToChange) {
        m_node = nodeToChange;
    }
    
    public final RefactoringStatus checkConditions(RefactoringStatus init) {
        if (m_node == null) {
            init.addFatalError("Change Error: node cannot be null!");
            return init;
        }
        
        if (m_node.isFrozen()) {
            init.addWarning("Node to be changed is frozen. Caution: context will be lost after"
                    + "change. New node is created from change method.");
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
     * This method is the runner for all changes. If the class
     * originally receives a node that is frozen, it will create
     * a copy of that node that is able to be changed. However, the
     * copy of that node means the outer context will be lost.
     * 
     * @return reference to the changed loop
     */
    @SuppressWarnings("unchecked")
    public final T change() {
        if (m_node.isFrozen()) {
            return doChange((T) m_node.copy());
        } else {
            return doChange(m_node);
        }
    }
    
    public T getOriginal() { return m_node; }
    
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
    
    public void setProgressMonitor(IProgressMonitor pm) {
        m_pm = pm;
    }
    public IProgressMonitor getProgressMonitor() {
        return m_pm;
    }
    
}
