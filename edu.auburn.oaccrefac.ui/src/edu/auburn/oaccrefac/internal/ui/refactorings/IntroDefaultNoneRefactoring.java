package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.IntroDefaultNoneAlteration;
import edu.auburn.oaccrefac.core.transformations.IntroDefaultNoneCheck;

public class IntroDefaultNoneRefactoring extends PragmaDirectiveRefactoring {

    private IntroDefaultNoneCheck check;
    
    public IntroDefaultNoneRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }
    
    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new IntroDefaultNoneCheck(getPragma());
        check.performChecks(status, pm, null);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new IntroDefaultNoneAlteration(rewriter, check).change();
    }

}
