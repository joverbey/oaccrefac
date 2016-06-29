package org.eclipse.ptp.pldt.openacc.internal.ui.actions.refactoring;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.LoopRefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.IntroRoutineRefactoring;

@SuppressWarnings("restriction")
public class IntroRoutineDelegate extends RefactoringActionDelegate {

	@Override
	public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
		return new IntroRoutineRefactoring(wc, selection, project);
	}

	@Override
	public RefactoringWizard createWizard(Refactoring refactoring) {
		return new LoopRefactoringWizard(refactoring, "Introduce OpenACC Routine", false);
	}

}
