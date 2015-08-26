package edu.auburn.oaccrefac.internal.core;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

public class InquisitorFactory {

    private static final Map<WeakReference<IASTForStatement>, ForStatementInquisitor> refs;

    static {
        refs = new HashMap<>();
    }

    public static ForStatementInquisitor getInquisitor(IASTForStatement context) {
        if (context == null)
            return null;

        Iterator<WeakReference<IASTForStatement>> it = refs.keySet().iterator();
        WeakReference<IASTForStatement> next = null;
        while (it.hasNext()) {
            next = it.next();
            if (next.get().equals(context)) {
                return refs.get(next);
            }
        }
        // Otherwise we need to create a new one
        ForStatementInquisitor value = ForStatementInquisitor.getInquisitor(context);
        WeakReference<IASTForStatement> key = new WeakReference<IASTForStatement>(context);
        refs.put(key, value);
        return value;
    }

    public static void delete(IASTForStatement context) {
        if (context == null)
            return;
        Iterator<WeakReference<IASTForStatement>> it = refs.keySet().iterator();
        WeakReference<IASTForStatement> next = null;

        while (it.hasNext()) {
            next = it.next();
            if (next.get().equals(context))
                refs.remove(context);
        }
    }

    public static void flush() {
        Iterator<WeakReference<IASTForStatement>> it = refs.keySet().iterator();
        WeakReference<IASTForStatement> next = null;
        while (it.hasNext()) {
            next = it.next();
            if (next.get() == null)
                refs.remove(next);
        }
    }

}
