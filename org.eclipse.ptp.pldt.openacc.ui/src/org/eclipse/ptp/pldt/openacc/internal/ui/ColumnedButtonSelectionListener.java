package org.eclipse.ptp.pldt.openacc.internal.ui;

import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.TileLoopsRefactoring;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ColumnedButtonSelectionListener extends ButtonSelectionListener implements SelectionListener {

	private Composite left;
	private Composite right;
	private TileLoopsRefactoring refac;
	
	public ColumnedButtonSelectionListener(TileLoopsRefactoring refac) {
		super();
		
		this.refac = refac;
	}

	public void setComposites(Composite left, Composite right) {
		this.left = left;
		this.right = right;
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (listenButton.getSelection()) {
			recursiveSetEnabled(left, true);
			recursiveSetEnabled(right, false);
			refac.setCut(false);
		}
		else {
			recursiveSetEnabled(left, false);
			recursiveSetEnabled(right, true);
			refac.setCut(true);
		}
	}
	
	private void recursiveSetEnabled(Control comp, boolean enabled) {
    	comp.setEnabled(enabled);
    	if (comp instanceof Composite) {
    		for (Control cont : ((Composite) comp).getChildren()) {
    			recursiveSetEnabled(cont, enabled);
    		}
    	}
    }
}
