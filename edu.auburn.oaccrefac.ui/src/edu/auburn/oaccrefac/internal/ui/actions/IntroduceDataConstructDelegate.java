package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.auburn.oaccrefac.internal.ui.LoopRefactoringWizard;
import edu.auburn.oaccrefac.internal.ui.refactorings.IntroduceDataConstructRefactoring;

@SuppressWarnings("restriction")
public class IntroduceDataConstructDelegate extends RefactoringActionDelegate {

    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new IntroduceDataConstructRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        if (!(refactoring instanceof IntroduceDataConstructRefactoring))
            throw new ClassCastException("Refactoring not IntroduceDataConstructRefactoring!");

        IntroduceDataConstructRefactoring refac = (IntroduceDataConstructRefactoring) refactoring;
        return new LoopRefactoringWizard(refac, "Introduce Default None " + "Refactoring");
    }

}
