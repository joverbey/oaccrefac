package edu.auburn.oaccrefac.core.dependence.check;

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;

/**
 * Class is designed to be the base class for all dependence analysis checks
 * that is needed for changes.
 * @author Adam Eichelkraut
 *
 */
public abstract class DependenceCheck {

    private IASTStatement[] m_statements;
    
    public DependenceCheck(final IASTStatement... statements) {
        m_statements = statements;
    }
    
    public abstract RefactoringStatus doCheck(RefactoringStatus status, DependenceAnalysis dep);
    
    public RefactoringStatus check(RefactoringStatus status, IProgressMonitor pm) {
        
        DependenceAnalysis dependenceAnalysis = null;
        try {
            dependenceAnalysis = new DependenceAnalysis(pm, m_statements);
        } catch (DependenceTestFailure e) {
            status.addError("Dependences could not be analyzed.  " + e.getMessage());
            return status;
        }
        
        return doCheck(status, dependenceAnalysis);
        
    }
    
}
