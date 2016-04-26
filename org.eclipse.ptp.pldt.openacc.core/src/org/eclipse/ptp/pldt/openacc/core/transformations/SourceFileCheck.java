package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;


public abstract class SourceFileCheck<T extends RefactoringParams> extends Check<T> {
    private final List<IASTPreprocessorPragmaStatement> statements;
    
    protected SourceFileCheck(List<IASTPreprocessorPragmaStatement> statements) {
        this.statements = statements;
    }
    
    protected void doPragmaFormCheck(RefactoringStatus status) { }
    
    public RefactoringStatus pragmaFormCheck(RefactoringStatus status, IProgressMonitor pm) {
        doPragmaFormCheck(status);
        return status;
    }
    
    @Override
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, T params) {
        super.performChecks(status, pm, params);
        if(status.hasFatalError()) {
            return status;
        }
        pragmaFormCheck(status, pm);
        if(status.hasFatalError()) {
            return status;
        }
        
        return status;
    } 
    
    public List<IASTPreprocessorPragmaStatement> getStatements() {
        return statements;
    }
}
