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
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.StripMineLoopRefactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class LoopRefactoringWizardPage extends UserInputWizardPage {

    private final ArrayList<String> controlLabels = new ArrayList<>();
    private final ArrayList<ValueChangedListener> controlListeners = new ArrayList<>();
    private StripMineLoopRefactoring refac;
    boolean button = false;

    public LoopRefactoringWizardPage(String name) {
        super(name);
    }

    public void addInputControl(String label, ValueChangedListener callback) {
        controlLabels.add(label);
        controlListeners.add(callback);
    }
    
    public void setButton(boolean button, StripMineLoopRefactoring refac) {
    	this.button = button;
    	this.refac = refac;
    }
    
    public boolean getButton() {
    	return button;
    }

    @Override
    public void createControl(Composite parent) {
        Composite c = new Composite(parent, SWT.NONE);
        c.setLayout(new GridLayout());
        ButtonComposite buttons = null;
        
        if (button) {
            buttons = new ButtonComposite(c, SWT.NONE, refac);
        }
                
        for (int i = 0; i < controlListeners.size(); i++) {
        	if (controlListeners.get(i) instanceof NumberValueChangedListener) {
        		NumberInputComposite nic = new NumberInputComposite(c, SWT.NONE, 
        				(NumberValueChangedListener) controlListeners.get(i));
        		nic.setLabelText(controlLabels.get(i));
        		if (button) {
            		buttons.addLabelChange(nic);
            	}
        	}
        	else if (controlListeners.get(i) instanceof StringValueChangedListener) {
        		StringInputComposite sic = new StringInputComposite(c, SWT.NONE, 
        				(StringValueChangedListener) controlListeners.get(i));
            	sic.setLabelText(controlLabels.get(i));
        	}
        }

        setControl(c);
        setTitle(getName());
        setPageComplete(true);
    }

}
