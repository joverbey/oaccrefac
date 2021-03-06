/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class RadioButtonComposite extends Composite {
	
	Button regButton;
	
	public RadioButtonComposite(Composite parent, int style, String label1, String label2, 
			RadioButtonSelectionListener regListener) {
		super(parent, style);
		
		setLayout(new GridLayout(2, false));
		
		regButton = new Button(this, SWT.RADIO);
		regButton.setText(label1);
		regListener.setButton(regButton);
		regButton.addSelectionListener(regListener);
        
        Button cutButton = new Button(this, SWT.RADIO);
        cutButton.setText(label2);
	}
	
	public void setFirst() {
        regButton.setSelection(true);
	}
}
