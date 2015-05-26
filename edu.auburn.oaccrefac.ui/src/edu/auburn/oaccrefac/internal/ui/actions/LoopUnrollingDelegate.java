package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.auburn.oaccrefac.internal.ui.LoopUnrollingWizard;
import edu.auburn.oaccrefac.internal.ui.refactorings.LoopUnrollingRefactoring;

@SuppressWarnings("restriction")
public class LoopUnrollingDelegate extends RefactoringActionDelegate {

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof ITextSelection) {
            setSelection((ITextSelection) selection);
        }
    }
    
    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new LoopUnrollingRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        return new LoopUnrollingWizard(refactoring);
    }
}
