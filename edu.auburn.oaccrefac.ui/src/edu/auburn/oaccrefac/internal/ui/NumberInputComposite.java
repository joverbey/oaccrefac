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

    public interface ValueChangedListener {
        public void valueChanged(int value);
    }
    
    private Label label;
    private Text inputText;
    private ValueChangedListener listener;
    
    public NumberInputComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(2, false));
        label = new Label(this, SWT.NONE);
        inputText = new Text(this, SWT.LEFT | SWT.BORDER);
        
        //Add a listener to make sure that the only thing inserted
        //into this text field are numbers.
        inputText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Text source = ((Text) e.getSource());
                String newText = source.getText();
                if (newText.length() > 0) {
                    char last = newText.charAt(newText.length()-1);
                    if (isNumber(last)) {
                        listener.valueChanged(Integer.parseInt(newText));
                    } else {
                        source.setText("");
                    }
                }
            }
        });
    }
    
    private boolean isNumber(char c) {
        if (c >= '0' && c <= '9') {
            return true;
        } else {
            return false;
        }
    }
    
    public void setListener(ValueChangedListener listener) {
        listener = listener;
    }
    
    public void setLabelText(String text) {
        label.setText(text);
    }
    public Label getLabel() {
        return label;
    }

}
