package edu.auburn.oaccrefac.core.dependence.check;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
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
public abstract class DependenceCheck extends Check {

    private IASTStatement[] m_statements;
    private DependenceAnalysis dependenceAnalysis;
    
    public DependenceCheck(final IASTForStatement loop) {
        if(loop.getBody() instanceof IASTCompoundStatement) {
            m_statements = ((IASTCompoundStatement) loop.getBody()).getStatements();
        }
        else {
            m_statements = new IASTStatement[1];
            m_statements[0] = loop.getBody();
        }
    }
    
    public abstract RefactoringStatus doCheck(RefactoringStatus status);
    
    @Override
    public RefactoringStatus check(RefactoringStatus status, IProgressMonitor pm) {
        
        try {
            dependenceAnalysis = new DependenceAnalysis(pm, m_statements);
        } catch (DependenceTestFailure e) {
            status.addError("Dependences could not be analyzed.  " + e.getMessage());
            return status;
        }
        
        return doCheck(status);
        
    }
    
    protected final DependenceAnalysis getDependenceAnalysis() {
        return dependenceAnalysis;
    }
}
