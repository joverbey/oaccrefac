/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NumberInputComposite extends Composite {

    private final Label label;

    public NumberInputComposite(Composite parent, int style, final ValueChangedListener listener) {
        super(parent, style);
        setLayout(new GridLayout(2, false));
        label = new Label(this, SWT.NONE);
        Text inputText = new Text(this, SWT.LEFT | SWT.BORDER);

        inputText.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e) {
                Text source = (Text) e.getSource();

                // get old text and create new text by using the VerifyEvent.text
                final String oldS = source.getText();
                String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
                
                e.doit = newS.matches("[0-9]*"); //$NON-NLS-1$
                if (e.doit) {
                    listener.valueChanged(Integer.parseInt(newS));
                }
            }
        });
    }

    public void setLabelText(String text) {
        label.setText(text);
    }

    public Label getLabel() {
        return label;
    }

    public interface ValueChangedListener {
        void valueChanged(int value);
    }
}
