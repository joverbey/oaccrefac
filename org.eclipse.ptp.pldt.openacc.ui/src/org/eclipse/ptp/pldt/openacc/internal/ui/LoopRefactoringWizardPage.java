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
package org.eclipse.ptp.pldt.openacc.internal.ui;

import java.util.ArrayList;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.ptp.pldt.openacc.internal.ui.NumberInputComposite.ValueChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class LoopRefactoringWizardPage extends UserInputWizardPage {

    private final ArrayList<String> controlLabels = new ArrayList<>();
    private final ArrayList<ValueChangedListener> controlListeners = new ArrayList<>();

    public LoopRefactoringWizardPage(String name) {
        super(name);
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
            NumberInputComposite nic = new NumberInputComposite(c, SWT.NONE, controlListeners.get(i));
            nic.setLabelText(controlLabels.get(i));
        }

        setControl(c);
        setTitle(getName());
        setPageComplete(true);
    }

}