package edu.auburn.oaccrefac.internal.core;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

public class InquisitorFactory {

    private static Map<WeakReference<IASTForStatement>, ForStatementInquisitor> m_refs;
    
    static {
        m_refs = new HashMap<>();
    }
    
    public static ForStatementInquisitor getInquisitor(IASTForStatement context) {
       if (context == null) return null;
       
       Iterator<WeakReference<IASTForStatement>> it = m_refs.keySet().iterator();
       WeakReference<IASTForStatement> next = null;
       while (it.hasNext()) {
           next = it.next();
           if (next.get().equals(context)) {
               return m_refs.get(next);
           }
       }
       //Otherwise we need to create a new one
       ForStatementInquisitor value = ForStatementInquisitor.getInquisitor(context);
       WeakReference<IASTForStatement> key = new WeakReference<IASTForStatement>(context);
       m_refs.put(key, value);
       return value;
    }
    
    public static void delete(IASTForStatement context) {
        if (context == null) return;
        Iterator<WeakReference<IASTForStatement>> it = m_refs.keySet().iterator();
        WeakReference<IASTForStatement> next = null;;
        while (it.hasNext()) {
            next = it.next();
            if (next.get().equals(context))
                m_refs.remove(context);
        }
    }
    
    public static void flush() {
        Iterator<WeakReference<IASTForStatement>> it = m_refs.keySet().iterator();
        WeakReference<IASTForStatement> next = null;
        while (it.hasNext()) {
            next = it.next();
            if (next.get() == null)
                m_refs.remove(next);
        }
    }
    
}