package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ColumnLayout;

import edu.auburn.oaccrefac.internal.ui.NumberInputComposite.ValueChangedListener;
import edu.auburn.oaccrefac.internal.ui.refactorings.LoopTilingRefactoring;

public class LoopTilingWizard extends RefactoringWizard {

    LoopTilingRefactoring m_refactoring;
    
    public LoopTilingWizard(LoopTilingRefactoring refactoring) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);

        m_refactoring = refactoring;
        setDefaultPageTitle("Distribute Loops");
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
            
            NumberInputComposite stripDepth = new NumberInputComposite(c, SWT.NONE);
            stripDepth.setLabelText("Strip Depth: ");
            stripDepth.setListener(new ValueChangedListener() {
                @Override
                public void valueChanged(int value) {
                    m_refactoring.setStripMineDepth(value);
                }
            });
            
            NumberInputComposite stripFactor = new NumberInputComposite(c, SWT.NONE);
            stripFactor.setLabelText("Strip Factor: ");
            stripDepth.setListener(new ValueChangedListener() {
                @Override
                public void valueChanged(int value) {
                    m_refactoring.setStripFactor(value);
                }
            });
            
            NumberInputComposite propagation = new NumberInputComposite(c, SWT.NONE);
            propagation.setLabelText("Interchange Propagation (-1 for arbitrary): ");
            stripDepth.setListener(new ValueChangedListener() {
                @Override
                public void valueChanged(int value) {
                    m_refactoring.setPropagateInterchange(value);
                }
            });
            
            setControl(c);
            setTitle(getName());
            setPageComplete(true);
        }
    }
}