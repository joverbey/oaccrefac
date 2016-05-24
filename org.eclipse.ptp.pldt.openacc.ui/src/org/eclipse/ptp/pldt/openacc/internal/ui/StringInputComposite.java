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

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;

public class StringInputComposite extends AbstractInputComposite {

    private StringValueChangedListener listener;

    public StringInputComposite(Composite parent, int style, final StringValueChangedListener listener) {
        super(parent, style);
        this.listener = listener;
    }

    @Override
    protected void valueChange(String newS, VerifyEvent e) {
    	e.doit = true; //any string input is valid
        if (e.doit) {
            listener.stringValueChanged(newS);
        }
    }
    
    public interface StringValueChangedListener extends ValueChangedListener {
        void stringValueChanged(String value);
    }
}
