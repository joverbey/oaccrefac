/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;

public class NumberInputComposite extends AbstractInputComposite {

	private NumberValueChangedListener listener;
	
    public NumberInputComposite(Composite parent, int style, final NumberValueChangedListener listener) {
        super(parent, style);
        this.listener = listener;
    }
    
    @Override
    protected void valueChange(String newS, VerifyEvent e) {
    	e.doit = newS.matches("[0-9]*"); //$NON-NLS-1$
        if (e.doit) {
            listener.valueChanged(Integer.parseInt(newS));
        }
    }

    public interface NumberValueChangedListener extends ValueChangedListener {
        void valueChanged(int value);
    }
}
