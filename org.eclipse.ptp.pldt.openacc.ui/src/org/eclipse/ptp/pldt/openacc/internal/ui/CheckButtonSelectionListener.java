package org.eclipse.ptp.pldt.openacc.internal.ui;

import org.eclipse.ptp.pldt.openacc.internal.ui.AbstractInputComposite.ValueChangedListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;

public abstract class CheckButtonSelectionListener implements SelectionListener, ValueChangedListener {

	private Button listenButton;
	private boolean defaultSelected;

	public CheckButtonSelectionListener(boolean defaultSelected) {
		this.defaultSelected = defaultSelected;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		toggleButton(listenButton.getSelection());
	}
	
	public void setButton(Button listenButton) {
		this.listenButton = listenButton;
		if (defaultSelected) {
			listenButton.setSelection(true);
		}
	}
	
	protected abstract void toggleButton(boolean selection);
	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {

	}

}
