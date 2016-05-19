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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;

public class ButtonSelectionListener implements SelectionListener {
	
	private LoopStripMiningRefactoring refac;
	private Button listenButton;
	private NumberInputComposite stringToChange;
	
	public ButtonSelectionListener(LoopStripMiningRefactoring refac, Button listenButton) {
		this.refac = refac;
		this.listenButton = listenButton;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (listenButton.getSelection()) {
			refac.setCut(false);
			stringToChange.setLabelText("Strip Size");
		}
		else {
			refac.setCut(true);
			stringToChange.setLabelText("Cut Size");
		}
	}
	
	public void addLabelChange(NumberInputComposite stringToChange) {
		this.stringToChange = stringToChange;
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
}