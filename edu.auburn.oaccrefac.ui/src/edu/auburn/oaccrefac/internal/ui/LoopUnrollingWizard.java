package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ColumnLayout;

import edu.auburn.oaccrefac.internal.ui.refactorings.LoopUnrollingRefactoring;

public class LoopUnrollingWizard extends RefactoringWizard {

    LoopUnrollingRefactoring m_refactoring;
    
    public LoopUnrollingWizard(LoopUnrollingRefactoring refactoring) {
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
            
            new Label(c, SWT.NONE).setText("Unroll Factor: ");
            Text inputUnroll = new Text(c, SWT.LEFT | SWT.BORDER);
            
            //Add a listener to make sure that the only thing inserted
            //into this text field are numbers.
            inputUnroll.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    Text source = ((Text) e.getSource());
                    String newText = source.getText();
                    if (newText.length() > 0) {
                        char last = newText.charAt(newText.length()-1);
                        if (isNumber(last)) {
                            m_refactoring.setUnrollFactor(Integer.parseInt(newText));
                        } else {
                            source.setText("");
                        }
                    }
                }
            });
            
            setControl(c);
            setTitle(getName());
            setPageComplete(true);
        }
    }
    
    private boolean isNumber(char c) {
        if (c >= '0' && c <= '9') {
            return true;
        } else {
            return false;
        }
    }

}
