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
	protected Button listenButton;
	
	public ButtonSelectionListener(ForLoopRefactoring refac) {
		this.refac = refac;
	}
	
	public ButtonSelectionListener() {
		
	}
	
	public void setButton(Button listenButton) {
		this.listenButton = listenButton;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (listenButton.getSelection()) {
			refac.setSecondOption(false);
		}
		else {
			refac.setSecondOption(true);
		}
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
}