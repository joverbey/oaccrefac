package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroduceAtomicsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroduceAtomicsCheck;

public class IntroduceAtomicsRefactoring extends StatementsRefactoring {

    private IntroduceAtomicsCheck check;

    public IntroduceAtomicsRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);

        if (selection == null || tu.getResource() == null || project == null) {
            initStatus.addFatalError("Invalid selection");
        }
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }

    @Override
    public void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new IntroduceAtomicsCheck(getStatements(), getAllEnclosedNodes());
        check.performChecks(initStatus, pm, null);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new IntroduceAtomicsAlteration(rewriter, check).change();
    }


}
