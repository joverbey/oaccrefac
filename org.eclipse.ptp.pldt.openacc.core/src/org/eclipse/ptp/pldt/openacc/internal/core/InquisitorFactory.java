/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core;

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
