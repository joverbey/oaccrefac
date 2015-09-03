package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class Check<T extends RefactoringParams> {

    protected void doParameterCheck(RefactoringStatus status, T params) { }
    
    public abstract IASTTranslationUnit getTranslationUnit();
    
    public RefactoringStatus parameterCheck(RefactoringStatus status, IProgressMonitor pm, T params) {
        doParameterCheck(status, params);
        return status;
    }
    
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, T params) {
        parameterCheck(status, pm, params);
        return status;
    }
    
}
