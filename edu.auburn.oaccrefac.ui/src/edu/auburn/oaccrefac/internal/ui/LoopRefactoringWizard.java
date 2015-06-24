package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class LoopRefactoringWizard extends RefactoringWizard {

    private String m_title;
    private LoopRefactoringWizardPage m_inputpage;
    
    public LoopRefactoringWizard(Refactoring refactoring, String title) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
        
        m_title = title;
        setDefaultPageTitle(m_title);
        setDialogSettings(CUIPlugin.getDefault().getDialogSettings());
        
        m_inputpage = new LoopRefactoringWizardPage(m_title);
    }
    
    @Override
    protected void addUserInputPages() {
        addPage(m_inputpage);
    }
    
    public LoopRefactoringWizardPage getInputPage() { return m_inputpage; }
}
