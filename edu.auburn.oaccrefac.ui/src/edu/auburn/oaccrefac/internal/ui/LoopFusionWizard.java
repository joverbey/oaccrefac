package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopFusionRefactoring;

public class LoopFusionWizard extends RefactoringWizard {

    LoopFusionRefactoring m_refactoring;
    
    public LoopFusionWizard(LoopFusionRefactoring refactoring) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);

        m_refactoring = refactoring;
        setDefaultPageTitle("Fuse Loops");
        setDialogSettings(CUIPlugin.getDefault().getDialogSettings());
    }

    @Override
    protected void addUserInputPages() {
    }
}   
