package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ColumnLayout;

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopFissionRefactoring;

public class LoopFissionWizard extends RefactoringWizard {

    LoopFissionRefactoring m_refactoring;
    
    public LoopFissionWizard(LoopFissionRefactoring refactoring) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);

        m_refactoring = refactoring;
        setDefaultPageTitle("Introduce OpenACC Parallel");
        setDialogSettings(CUIPlugin.getDefault().getDialogSettings());
    }

    @Override
    protected void addUserInputPages() {
        addPage(new EmptyPage());
    }
    
    private class EmptyPage extends UserInputWizardPage {
        
        public EmptyPage() {
            super("(empty)");
        }

        @Override
        public void createControl(Composite parent) {
            Composite c = new Composite(parent, SWT.NONE);
            c.setLayout(new ColumnLayout());
            
            setControl(c);
            setTitle(getName());
            setPageComplete(true);
        }
    }
}   
    
