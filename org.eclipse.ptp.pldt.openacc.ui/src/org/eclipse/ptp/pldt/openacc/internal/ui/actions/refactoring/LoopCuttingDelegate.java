package org.eclipse.ptp.pldt.openacc.internal.ui.actions.refactoring;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.LoopRefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.NumberInputComposite.ValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.LoopCuttingRefactoring;

@SuppressWarnings("restriction")
public class LoopCuttingDelegate extends RefactoringActionDelegate {

    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new LoopCuttingRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        final LoopCuttingRefactoring refac = (LoopCuttingRefactoring) refactoring;
        LoopRefactoringWizard gui = new LoopRefactoringWizard(refactoring, "Cut Loops");
        gui.getInputPage().addNumberInputControl("Cut Size", new ValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setCutFactor(value);
            }
        });
        return gui;
    }
}
