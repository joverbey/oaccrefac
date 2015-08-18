package edu.auburn.oaccrefac.core.dependence.check;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class Check {

    protected abstract RefactoringStatus doCheck(RefactoringStatus status);
    
    public RefactoringStatus check(RefactoringStatus status, IProgressMonitor pm) {
        return doCheck(status);
    }
    
}
