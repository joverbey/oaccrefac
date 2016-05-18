package org.eclipse.ptp.pldt.openacc.internal.ui.actions.refactoring;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.LoopRefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.LoopRefactoringWizardPage;
import org.eclipse.ptp.pldt.openacc.internal.ui.NumberInputComposite.NumberValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.StringInputComposite.StringValueChangedListener;
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
        LoopRefactoringWizard wizard = new LoopRefactoringWizard(refactoring, "Cut Loops");
        LoopRefactoringWizardPage page = wizard.getInputPage();
        page.addInputControl("Cut Size", new NumberValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setCutFactor(value);
            }
        });
        page.addInputControl("Index Variable Name", new StringValueChangedListener() {
            @Override
            public void stringValueChanged(String value) {
                refac.setNewName(value);
            }
        });
        return wizard;
    }
}
