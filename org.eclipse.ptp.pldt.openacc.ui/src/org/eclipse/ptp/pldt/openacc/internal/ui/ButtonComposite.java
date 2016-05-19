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

import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.LoopStripMiningRefactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class ButtonComposite extends Composite {
	
	private ButtonSelectionListener regListener;

	public ButtonComposite(Composite parent, int style, LoopStripMiningRefactoring refac) {
		super(parent, style);
		
		setLayout(new GridLayout(2, false));
		
		Button regButton = new Button(this, SWT.RADIO);
		regButton.setText("Regular");
		regListener = new ButtonSelectionListener(refac, regButton);
		regButton.addSelectionListener(regListener);
        regButton.setSelection(true);
        
        Button cutButton = new Button(this, SWT.RADIO);
        cutButton.setText("Cutting");
        
	}
	
	public void addLabelChange(NumberInputComposite nic) {
		regListener.addLabelChange(nic);
	}
}
