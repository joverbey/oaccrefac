package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopFissionRefactoring;

public class LoopFissionWizard extends RefactoringWizard {

    LoopFissionRefactoring m_refactoring;
    
    public LoopFissionWizard(LoopFissionRefactoring refactoring) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);

        m_refactoring = refactoring;
        setDefaultPageTitle("Distribute Loops");
        setDialogSettings(CUIPlugin.getDefault().getDialogSettings());
    }

    @Override
    protected void addUserInputPages() {
    }
}