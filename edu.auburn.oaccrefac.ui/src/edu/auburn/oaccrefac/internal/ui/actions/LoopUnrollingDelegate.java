package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.auburn.oaccrefac.internal.ui.LoopRefactoringWizard;
import edu.auburn.oaccrefac.internal.ui.NumberInputComposite.ValueChangedListener;
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
        if (!(refactoring instanceof LoopUnrollingRefactoring))
            throw new ClassCastException("Refactoring not LoopUnrollingRefactoring!");
        final LoopUnrollingRefactoring refac = (LoopUnrollingRefactoring) refactoring;
        LoopRefactoringWizard weasley = 
                new LoopRefactoringWizard(refactoring, "LoopUnrollingRefactoring");
        weasley.getInputPage().addNumberInputControl("Unroll Factor", new ValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setUnrollFactor(value);
                
            }
        });
        return weasley;
    }
}
