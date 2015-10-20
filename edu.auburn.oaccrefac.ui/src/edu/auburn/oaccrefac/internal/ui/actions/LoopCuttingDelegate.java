package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.auburn.oaccrefac.internal.ui.LoopRefactoringWizard;
import edu.auburn.oaccrefac.internal.ui.NumberInputComposite.ValueChangedListener;
import edu.auburn.oaccrefac.internal.ui.refactorings.LoopCuttingRefactoring;

@SuppressWarnings("restriction")
public class LoopCuttingDelegate extends RefactoringActionDelegate{

    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new LoopCuttingRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        if (!(refactoring instanceof LoopCuttingRefactoring))
            throw new ClassCastException("Refactoring not LoopCuttingRefactoring!");
        final LoopCuttingRefactoring refac = (LoopCuttingRefactoring) refactoring;
        LoopRefactoringWizard GUI = 
                new LoopRefactoringWizard(refactoring, "LoopCuttingRefactoring");
        GUI.getInputPage().addNumberInputControl("Cut Factor", new ValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setCutFactor(value);
                
            }
        });
        return GUI;
        
    }

}
