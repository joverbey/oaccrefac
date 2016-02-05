package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingAlteration;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingCheck;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingParams;

public class LoopCuttingRefactoring extends ForLoopRefactoring{

    private int cutFactor;
    private LoopCuttingCheck check;
    
    // TODO: put a good comment here
    public LoopCuttingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    public void setCutFactor(int factor) {
        this.cutFactor = factor;
    }
    
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new LoopCuttingCheck(getLoop());
        check.performChecks(status, pm, new LoopCuttingParams(cutFactor));
    }
    
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new LoopCuttingAlteration(rewriter, cutFactor, check).change();
    }
}
