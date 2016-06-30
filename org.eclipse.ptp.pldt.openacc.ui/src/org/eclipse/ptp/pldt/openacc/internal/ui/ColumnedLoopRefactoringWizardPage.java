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

import java.util.ArrayList;

import org.eclipse.ptp.pldt.openacc.internal.ui.AbstractInputComposite.ValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.NumberInputComposite.NumberValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.StringInputComposite.StringValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.TileLoopsRefactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ColumnedLoopRefactoringWizardPage extends LoopRefactoringWizardPage {

	private final ArrayList<String> leftControlLabels = new ArrayList<>();
    private final ArrayList<ValueChangedListener> leftControlListeners = new ArrayList<>();
    private final ArrayList<String> rightControlLabels = new ArrayList<>();
    private final ArrayList<ValueChangedListener> rightControlListeners = new ArrayList<>();
    private TileLoopsRefactoring refac;
	
	public ColumnedLoopRefactoringWizardPage(String name, TileLoopsRefactoring refac) {
		super(name);
		this.refac = refac;
	}
	
	public void addLeftInputControl(String label, ValueChangedListener callback) {
        leftControlLabels.add(label);
        leftControlListeners.add(callback);
    }
	
	public void addRightInputControl(String label, ValueChangedListener callback) {
        rightControlLabels.add(label);
        rightControlListeners.add(callback);
    }

	@Override
	public void createControl(Composite parent) {
		Composite c = new Composite (parent, SWT.NONE);
		c.setLayout(new GridLayout());
		ColumnedButtonSelectionListener listener = new ColumnedButtonSelectionListener(refac);
		RadioButtonComposite buttons = new RadioButtonComposite(c, SWT.NONE, Messages.ColumnedLoopRefactoringWizardPage_Tiling, Messages.ColumnedLoopRefactoringWizardPage_Cutting, listener);
		Composite bottom = new Composite(c, SWT.NONE);
		bottom.setLayout(new GridLayout(2, true));
		Composite left = new Composite(bottom, SWT.RIGHT_TO_LEFT);
        left.setLayout(new GridLayout());
		Composite right = new Composite(bottom, SWT.RIGHT_TO_LEFT);
		right.setLayout(new GridLayout());
		listener.setComposites(left, right);
        
		for (int i = 0; i < leftControlListeners.size(); i++) {
        	if (leftControlListeners.get(i) instanceof NumberValueChangedListener) {
        		NumberInputComposite nic = new NumberInputComposite(left, SWT.LEFT_TO_RIGHT, 
        				(NumberValueChangedListener) leftControlListeners.get(i));
        		nic.setLabelText(leftControlLabels.get(i));
        		
        	} else if (leftControlListeners.get(i) instanceof StringValueChangedListener) {
        		StringInputComposite sic = new StringInputComposite(left, SWT.LEFT_TO_RIGHT, 
        				(StringValueChangedListener) leftControlListeners.get(i));
            	sic.setLabelText(leftControlLabels.get(i));
        	}
		}
		
		for (int i = 0; i < rightControlListeners.size(); i++) {
        	if (rightControlListeners.get(i) instanceof NumberValueChangedListener) {
        		NumberInputComposite nic = new NumberInputComposite(right, SWT.LEFT_TO_RIGHT, 
        				(NumberValueChangedListener) rightControlListeners.get(i));
        		nic.setLabelText(rightControlLabels.get(i));
        		
        	} else if (rightControlListeners.get(i) instanceof StringValueChangedListener) {
        		StringInputComposite sic = new StringInputComposite(right, SWT.LEFT_TO_RIGHT, 
        				(StringValueChangedListener) rightControlListeners.get(i));
            	sic.setLabelText(rightControlLabels.get(i));
        	}
		}
		
		recursiveSetEnabled(right, false);
		
		setControl(c);
        setTitle(getName());
        setPageComplete(true);
		buttons.setFirst();
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
