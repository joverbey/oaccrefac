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
import edu.auburn.oaccrefac.internal.ui.refactorings.LoopStripMiningRefactoring;

@SuppressWarnings("restriction")
public class LoopStripMiningDelegate extends RefactoringActionDelegate {

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof ITextSelection) {
            setSelection((ITextSelection) selection);
        }
    }
    
    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new LoopStripMiningRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        if (!(refactoring instanceof LoopStripMiningRefactoring))
            throw new ClassCastException("Refactoring not LoopStripMiningRefactoring!");
        final LoopStripMiningRefactoring refac = (LoopStripMiningRefactoring) refactoring;
        LoopRefactoringWizard hermione = 
                new LoopRefactoringWizard(refactoring, "LoopStripMiningRefactoring");
        hermione.getInputPage().addNumberInputControl("Strip Factor", new ValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setStripFactor(value);
                
            }
        });
        return hermione;
    }

}
