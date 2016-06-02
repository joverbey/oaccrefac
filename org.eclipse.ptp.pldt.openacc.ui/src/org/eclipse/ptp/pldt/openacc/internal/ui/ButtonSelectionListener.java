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

import org.eclipse.ptp.pldt.openacc.internal.ui.AbstractInputComposite.ValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.ForLoopRefactoring;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;

public class ButtonSelectionListener implements SelectionListener, ValueChangedListener {
	
	private ForLoopRefactoring refac;
	private Button listenButton;
	private NumberInputComposite stringToChange;
	private String label1, label2;
	
	public ButtonSelectionListener(ForLoopRefactoring refac, String label1, String label2) {
		this.refac = refac;
		this.label1 = label1;
		this.label2 = label2;
	}
	
	public void setButton(Button listenButton) {
		this.listenButton = listenButton;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (listenButton.getSelection()) {
			refac.setSecondOption(false);
			stringToChange.setLabelText(label1);
		}
		else {
			refac.setSecondOption(true);
			stringToChange.setLabelText(label2);
		}
	}
	
	public void addLabelChange(NumberInputComposite stringToChange) {
		this.stringToChange = stringToChange;
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
}