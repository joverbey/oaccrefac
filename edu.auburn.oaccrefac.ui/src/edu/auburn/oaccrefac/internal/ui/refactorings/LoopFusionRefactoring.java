package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.SourceAlteration;
import edu.auburn.oaccrefac.core.transformations.Check;
import edu.auburn.oaccrefac.core.transformations.FuseLoopsAlteration;
import edu.auburn.oaccrefac.core.transformations.FuseLoopsInitialCheck;
import edu.auburn.oaccrefac.core.transformations.IASTRewrite;

/**
 * This class defines the implementation for re-factoring using loop fusion. For example:
 * 
 * ORIGINAL:                  REFACTORED: 
 * int i, a[100], b[100];    | int i, a[100], b[100]; 
 * for (i = 0; i < 100; i++) | for (i = 0; i <100; i++)
 *    a[i] = 1;              | { 
 * for (i = 0; i < 100; i++) | a[i] = 1; 
 *    b[i] = 2;              | b[i] = 2; 
 *                           | }
 * 
 * (Example taken from Wikipedia's web page on loop Fusion)
 */
public class LoopFusionRefactoring extends ForLoopRefactoring {

    public LoopFusionRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        Check check = new FuseLoopsInitialCheck(getLoop());
        check.check(initStatus, pm);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {
        SourceAlteration change = new FuseLoopsAlteration(getAST(), rewriter, getLoop());
        change.change();
    }

}
