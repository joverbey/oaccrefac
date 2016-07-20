/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui;

import java.util.ArrayList;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.ptp.pldt.openacc.internal.ui.AbstractInputComposite.ValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.NumberInputComposite.NumberValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.StringInputComposite.StringValueChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class LoopRefactoringWizardPage extends UserInputWizardPage {

    private final ArrayList<String> controlLabels = new ArrayList<>();
    private final ArrayList<ValueChangedListener> controlListeners = new ArrayList<>();
    private final ArrayList<Composite> composites = new ArrayList<Composite>();

    public LoopRefactoringWizardPage(String name) {
        super(name);
    }

    public void addInputControl(String label, ValueChangedListener callback) {
        controlLabels.add(label);
        controlListeners.add(callback);
    }

    @Override
    public void createControl(Composite parent) {
    	Composite master = new Composite(parent, SWT.NONE);
        master.setLayout(new GridLayout());
        Composite c = new Composite(master, SWT.RIGHT_TO_LEFT);
        c.setLayout(new GridLayout());
        RadioButtonComposite radioButtons = null;
        CheckButtonComposite checkButtons = null;
        boolean hasButtons = false;
                
        for (int i = 0; i < controlListeners.size(); i++) {
        	if (controlListeners.get(i) instanceof NumberValueChangedListener) {
        		NumberInputComposite nic = new NumberInputComposite(c, SWT.LEFT_TO_RIGHT, 
        				(NumberValueChangedListener) controlListeners.get(i));
        		nic.setLabelText(controlLabels.get(i));
        		composites.add(nic);
        	}
        	else if (controlListeners.get(i) instanceof StringValueChangedListener) {
        		StringInputComposite sic = new StringInputComposite(c, SWT.LEFT_TO_RIGHT, 
        				(StringValueChangedListener) controlListeners.get(i));
            	sic.setLabelText(controlLabels.get(i));
            	composites.add(sic);
        	}
        	else if (controlListeners.get(i) instanceof RadioButtonSelectionListener) {
        		if (!hasButtons) {
        			hasButtons = true;
        			radioButtons = new RadioButtonComposite(c, SWT.LEFT_TO_RIGHT, Messages.LoopRefactoringWizardPage_Parallel, controlLabels.get(i), 
        					(RadioButtonSelectionListener) controlListeners.get(i));
        		}
        	} else if (controlListeners.get(i) instanceof CheckButtonSelectionListener) {
        		if (checkButtons == null) {
        			checkButtons = new CheckButtonComposite(master, SWT.LEFT_TO_RIGHT);
        		}
        		CheckButtonSelectionListener listener = (CheckButtonSelectionListener) controlListeners.get(i);
				checkButtons.addButton(listener, controlLabels.get(i));
				if (listener.getCompIndex() >= 0) {
					listener.setComp(composites.get(listener.getCompIndex()));
				}
        	}
        }
        
        if (hasButtons) {
        	radioButtons.setFirst();
        }
        if (!composites.isEmpty()) {
        	composites.get(0).setFocus();
        }
        setControl(master);
        setTitle(getName());
        setPageComplete(true);
    }
    
    

}
