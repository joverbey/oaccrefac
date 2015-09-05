/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.ui;

import java.util.ArrayList;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import edu.auburn.oaccrefac.internal.ui.NumberInputComposite.ValueChangedListener;

public class LoopRefactoringWizardPage extends UserInputWizardPage {
    
    private ArrayList<String> controlLabels;
    private ArrayList<ValueChangedListener> controlListeners;

    public LoopRefactoringWizardPage(String name) {
        super(name);
        controlLabels = new ArrayList<>();
        controlListeners = new ArrayList<>();
    }
    
    public void addNumberInputControl(String label, ValueChangedListener callback) {
        controlLabels.add(label);
        controlListeners.add(callback);
    }

    @Override
    public void createControl(Composite parent) {
        Composite c = new Composite(parent, SWT.NONE);
        c.setLayout(new GridLayout());
        
        for (int i = 0; i < controlListeners.size(); i++) {
            NumberInputComposite nic = new NumberInputComposite(c, SWT.NONE);
            nic.setLabelText(controlLabels.get(i));
            nic.setListener(controlListeners.get(i));
        }
        
        setControl(c);
        setTitle(getName());
        setPageComplete(true);
    }

}
