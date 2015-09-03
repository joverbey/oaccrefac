package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.DistributeLoopsAlteration;
import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.DistributeLoopsCheck;

/**
 * This class defines the implementation for re-factoring using loop fusion. For example:
 * 
 * ORIGINAL:                  REFACTORED: 
 * int i, a[100], b[100];     | int i, a[100], b[100]; 
 * for (i = 0; i < 100; i++) {| for (i = 0; i <100; i++)
 *    a[i] = 1;               |     a[i] = 1;
 *    b[i] = 2;               | for (i = 0; i <100; i++)
 * }                          |     b[i] = 2; 
 *                            |
 * 
 * (Example taken from Wikipedia's web page on loop Fission.)
 */
public class LoopFissionRefactoring extends ForLoopRefactoring {

    private DistributeLoopsCheck check;
    
    public LoopFissionRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new DistributeLoopsCheck(this.getLoop());
        check.performChecks(status, pm, null);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {
        new DistributeLoopsAlteration(getAST(), rewriter, getLoop(), check).change();
    }
}
