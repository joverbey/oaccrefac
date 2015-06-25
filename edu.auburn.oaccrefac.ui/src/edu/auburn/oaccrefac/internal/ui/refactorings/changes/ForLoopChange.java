package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;

/**
 * This class defines the base strategy interface to be derived
 * from for changes made to a for loop.
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopChange {
    
    private IASTForStatement m_loop;
    private IProgressMonitor m_pm;
    
    public ForLoopChange(IASTForStatement loop) {
        m_loop = loop;
    }
    
    /**
     * Abstract method describes the implementation that all changes must
     * define. This method takes in a loop and changes it in respect to
     * it's intended purpose.
     * @param loop -- the loop in which to change
     * @return reference to changed loop
     */
    protected abstract IASTForStatement doChange(IASTForStatement loop);
    
    /**
     * Abstract method for checking the initial conditions for change objects. For
     * example, in this method, it will check the inputs to the constructor to ensure
     * that the inputs are valid.
     * @param init -- Status object that may or may not be changed depending on error
     * @return -- Reference to changed status object
     */
    protected abstract RefactoringStatus doCheckConditions(RefactoringStatus init);
    
    /**
     * This method is the runner for all changes.
     * 
     * @return reference to the changed loop
     */
    public final IASTForStatement change() {
        if (m_loop != null) {
            if (m_loop.isFrozen()) {
                return doChange(m_loop.copy());
            } else {
                return doChange(m_loop);
            }
        } else {
            return null;
        }
    }
    
    public final RefactoringStatus checkConditions(RefactoringStatus init) {
        if (m_loop == null) {
            init.addFatalError("For Loop Change -- Error: loop cannot be null!");
            return init;
        }
        
        return doCheckConditions(init);
    }
    
    /**
     * The doChange function receives a non-frozen version of the
     * original loop. For the instances where the original loop is
     * frozen, you can use this method to receive the original loop.
     * 
     * The instances where the loop is not frozen, the doChange loop
     * receives the original loop to modify.
     * @return
     */
    public IASTForStatement getOriginal() { return m_loop; }
    
    public void setProgressMonitor(IProgressMonitor pm) {
        m_pm = pm;
    }
    public IProgressMonitor getProgressMonitor() {
        return m_pm;
    }
    
    protected DependenceAnalysis performDependenceAnalysis(RefactoringStatus status, IProgressMonitor pm, IASTStatement... statements) {
        try {
            return new DependenceAnalysis(pm, statements);
        } catch (DependenceTestFailure e) {
            status.addError("Dependences could not be analyzed.  " + e.getMessage());
            return null;
        }
    }
    
}
