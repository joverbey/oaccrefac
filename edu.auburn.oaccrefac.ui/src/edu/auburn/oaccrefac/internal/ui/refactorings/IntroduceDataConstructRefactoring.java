package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.IntroduceDataConstructAlteration;
import edu.auburn.oaccrefac.core.transformations.IntroduceDataConstructCheck;

@SuppressWarnings("restriction")
public class IntroduceDataConstructRefactoring extends StatementsRefactoring {

    private IntroduceDataConstructCheck check;
    
    public IntroduceDataConstructRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        
        if (selection == null || tu.getResource() == null || project == null)
            initStatus.addFatalError("Invalid selection");
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }
    
    @Override
    public void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new IntroduceDataConstructCheck(getStatements());
        check.performChecks(initStatus, pm, null);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new IntroduceDataConstructAlteration(rewriter, check).change();
    }

}
