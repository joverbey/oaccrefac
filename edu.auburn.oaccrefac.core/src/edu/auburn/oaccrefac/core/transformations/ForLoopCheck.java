package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;

public class ForLoopCheck<T extends RefactoringParameters> extends Check<T> {

    protected final IASTForStatement loop;
    
    protected ForLoopCheck(IASTForStatement loop) {
        this.loop = loop;
    }
    
    protected void doLoopFormCheck(RefactoringStatus status) { }
    
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) { }

    public RefactoringStatus loopFormCheck(RefactoringStatus status, IProgressMonitor pm) {
        doLoopFormCheck(status);
        return status;
    }
    
    public final RefactoringStatus dependenceCheck(RefactoringStatus status, IProgressMonitor pm) {
        
        IASTStatement[] statements;
        DependenceAnalysis dependenceAnalysis;
        
        if (loop.getBody() instanceof IASTCompoundStatement) {
            statements = ((IASTCompoundStatement) loop.getBody()).getStatements();
        } else {
            statements = new IASTStatement[1];
            statements[0] = loop.getBody();
        }
        
        try {
            dependenceAnalysis = new DependenceAnalysis(pm, statements);
        } catch (DependenceTestFailure e) {
            status.addError("Dependences could not be analyzed.  " + e.getMessage());
            return status;
        }
        doDependenceCheck(status, dependenceAnalysis);
        return status;
    }
    
    @Override
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, T params) {
        super.performChecks(status, pm, params);
        loopFormCheck(status, pm);
        dependenceCheck(status, pm);
        return status;
    }

    public IASTForStatement getLoop() {
        return loop;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return loop.getTranslationUnit();
    }
    
}
