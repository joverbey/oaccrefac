package edu.auburn.oaccrefac.internal.ui;

import java.util.ArrayList;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class LoopRefactoringWizard extends RefactoringWizard {

    private String m_title;
    private ArrayList<IWizardPage> m_pagesToAdd;
    
    public LoopRefactoringWizard(Refactoring refactoring, String title) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
        
        m_title = title;
        setDefaultPageTitle(m_title);
        setDialogSettings(CUIPlugin.getDefault().getDialogSettings());
        
        m_pagesToAdd = new ArrayList<>();
    }
    
    public void addRefactoringPage(LoopRefactoringWizardPage page) {
        m_pagesToAdd.add(page);
    }
    
    @Override
    protected void addUserInputPages() {
        for (IWizardPage page : m_pagesToAdd) {
            addPage(page);
        }
    }
}
