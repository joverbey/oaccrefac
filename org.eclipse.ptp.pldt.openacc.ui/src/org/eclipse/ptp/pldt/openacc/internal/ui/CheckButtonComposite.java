package org.eclipse.ptp.pldt.openacc.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class CheckButtonComposite extends Composite {

	public CheckButtonComposite(Composite parent, int style) {
		super(parent, style);
		
		setLayout(new GridLayout(2, false));
	}
	
	public void addButton(CheckButtonSelectionListener listener, String label) {
		Button button = new Button(this, SWT.CHECK);
		button.addSelectionListener(listener);
		button.setText(label);
		listener.setButton(button);
	}
}
