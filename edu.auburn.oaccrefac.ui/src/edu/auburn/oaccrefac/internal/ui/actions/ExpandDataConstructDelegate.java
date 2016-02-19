package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.auburn.oaccrefac.internal.ui.LoopRefactoringWizard;
import edu.auburn.oaccrefac.internal.ui.refactorings.ExpandDataConstructRefactoring;
import edu.auburn.oaccrefac.internal.ui.refactorings.IntroduceDataConstructRefactoring;

@SuppressWarnings("restriction")
public class ExpandDataConstructDelegate extends RefactoringActionDelegate {

    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new ExpandDataConstructRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        if (!(refactoring instanceof ExpandDataConstructRefactoring))
            throw new ClassCastException("Refactoring not ExpandDataConstructRefactoring!");
        
        //ExpandDataConstructRefactoring refac = (ExpandDataConstructRefactoring) refactoring;
        LoopRefactoringWizard wiz = new LoopRefactoringWizard(refactoring, "Expand Default None Refactoring");
        return wiz;
    }

}
