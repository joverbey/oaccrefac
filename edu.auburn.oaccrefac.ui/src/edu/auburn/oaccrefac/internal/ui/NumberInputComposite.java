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
package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NumberInputComposite extends Composite {

    private final Label label;
    private final ValueChangedListener listener;

    public NumberInputComposite(Composite parent, int style, ValueChangedListener listener) {
        super(parent, style);
        setLayout(new GridLayout(2, false));
        label = new Label(this, SWT.NONE);
        Text inputText = new Text(this, SWT.LEFT | SWT.BORDER);
        this.listener = listener;

        // Ensure that only numbers are added to the text field.
        inputText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Text source = (Text) e.getSource();
                String newText = source.getText();

                if (!newText.matches("[0-9]*")) {
                    source.setText("");
                    // TODO: Should we update the listener's value when we
                    // reset the text to ""?
                } else {
                    NumberInputComposite.this.listener.valueChanged(Integer.parseInt(newText));
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
