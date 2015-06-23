package edu.auburn.oaccrefac.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NumberInputComposite extends Composite {

    public interface ValueChangedListener {
        public void valueChanged(int value);
    }
    
    private Label m_label;
    private Text m_inputText;
    private ValueChangedListener m_listener;
    
    public NumberInputComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new RowLayout());
        m_label = new Label(this, SWT.NONE);
        m_inputText = new Text(this, SWT.LEFT | SWT.BORDER);
        
        //Add a listener to make sure that the only thing inserted
        //into this text field are numbers.
        m_inputText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                Text source = ((Text) e.getSource());
                String newText = source.getText();
                if (newText.length() > 0) {
                    char last = newText.charAt(newText.length()-1);
                    if (isNumber(last)) {
                        m_listener.valueChanged(Integer.parseInt(newText));
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
        m_listener = listener;
    }
    
    public void setLabelText(String text) {
        m_label.setText(text);
    }
    public Label getLabel() {
        return m_label;
    }

}
