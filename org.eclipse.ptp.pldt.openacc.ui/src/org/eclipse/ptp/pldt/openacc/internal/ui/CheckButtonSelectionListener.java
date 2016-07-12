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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public abstract class CheckButtonSelectionListener implements SelectionListener, ValueChangedListener {

	private Button listenButton;
	private boolean defaultSelected;
	private int compIndex;
	private Composite comp;

	public CheckButtonSelectionListener(boolean defaultSelected) {
		this.defaultSelected = defaultSelected;
		this.compIndex = -1;
	}
	
	public CheckButtonSelectionListener(boolean defaultSelected, int compositeToAffect) {
		this.defaultSelected = defaultSelected;
		this.compIndex = compositeToAffect;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		toggleButton(listenButton.getSelection());
		if (compIndex >= 0) {
			compAffect(comp, listenButton.getSelection());
		}
	}
	
	public void setButton(Button listenButton) {
		this.listenButton = listenButton;
		if (defaultSelected) {
			listenButton.setSelection(true);
		}
	}
	
	public void setCompIndex(int compToAffect) {
		this.compIndex = compToAffect;
	}
	
	public void setComp(Composite comp) {
		this.comp = comp;
		compAffect(comp, listenButton.getSelection());
	}
	
	public int getCompIndex() {
		return compIndex;
	}
	
	protected void compAffect(Composite comp, boolean selection) {
		
	}
	
	protected abstract void toggleButton(boolean selection);
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {

	}

}
