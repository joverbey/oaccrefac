package edu.auburn.oaccrefac.internal.ui;

import java.util.ArrayList;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import edu.auburn.oaccrefac.internal.ui.NumberInputComposite.ValueChangedListener;

public class LoopRefactoringWizardPage extends UserInputWizardPage {
    
    private ArrayList<String> m_controlLabels;
    private ArrayList<ValueChangedListener> m_controlListeners;

    public LoopRefactoringWizardPage(String name) {
        super(name);
        m_controlLabels = new ArrayList<>();
        m_controlListeners = new ArrayList<>();
    }
    
    public void addNumberInputControl(String label, ValueChangedListener callback) {
        m_controlLabels.add(label);
        m_controlListeners.add(callback);
    }

    @Override
    public void createControl(Composite parent) {
        Composite c = new Composite(parent, SWT.NONE);
        c.setLayout(new GridLayout());
        
        for (int i = 0; i < m_controlListeners.size(); i++) {
            NumberInputComposite nic = new NumberInputComposite(c, SWT.NONE);
            nic.setLabelText(m_controlLabels.get(i));
            nic.setListener(m_controlListeners.get(i));
        }
        
        setControl(c);
        setTitle(getName());
        setPageComplete(true);
    }

}
