/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.constprop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ConstantPropagation;

/**
 * A constant environment.
 * <p>
 * A constant environment maps a variable ({@link IBinding}) to an {@link Long} representing its constant value at a
 * particular point in a program, or <code>null</code> if it is not constant-valued.
 * <p>
 * Used by {@link ConstantPropagation}.
 */
public class ConstEnv {
    /** An empty constant environment, where no variables are constant-valued. */
    public static final ConstEnv EMPTY = new ConstEnv(Collections.<IBinding, Long> emptyMap());

    private final Map<IBinding, Long> env;

    private ConstEnv(Map<IBinding, Long> values) {
        this.env = Collections.unmodifiableMap(values);
    }

    public ConstEnv intersect(ConstEnv that) {
        if (that == null)
            return this;

        Map<IBinding, Long> result = new HashMap<IBinding, Long>();
        for (IBinding b : this.env.keySet()) {
            Long thatValue = that.env.get(b);
            if (this.env.get(b).equals(thatValue)) {
                result.put(b, thatValue);
            }
        }
        return new ConstEnv(result);
    }

    public ConstEnv without(IVariable var) {
        Map<IBinding, Long> result = new HashMap<IBinding, Long>();
        for (IBinding b : this.env.keySet()) {
            if (!b.equals(var)) {
                result.put(b, this.env.get(b));
            }
        }
        return new ConstEnv(result);
    }

    public ConstEnv set(IBinding variable, Long value) {
        if (!ConstantPropagation.canTrackConstantValues(variable))
            throw new IllegalArgumentException("Cannot track constant values for " + variable //$NON-NLS-1$
                    + ".  Invoke canTrackConstantValues() before calling set()."); //$NON-NLS-1$
        if (!ConstantPropagation.isInTrackedRange(variable, value))
            throw new IllegalArgumentException(
                    "Value is out of range for " + variable + ".  Invoke isInTrackedRange() before calling set()."); //$NON-NLS-1$ //$NON-NLS-2$

        Map<IBinding, Long> updated = new HashMap<IBinding, Long>(this.env);
        if (value == null)
            updated.remove(variable);
        else
            updated.put(variable, value);
        return new ConstEnv(updated);
    }

    public Long getValue(IBinding binding) {
        return this.env.get(binding);
    }

    @Override
    public int hashCode() {
        return env.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConstEnv other = (ConstEnv) obj;
        if (env == null) {
            if (other.env != null)
                return false;
        } else if (!env.equals(other.env))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (IBinding b : this.env.keySet()) {
            if (!first)
                sb.append(", "); //$NON-NLS-1$
            first = false;
            sb.append(b);
            sb.append(" -> "); //$NON-NLS-1$
            sb.append(this.env.get(b));
        }
        return sb.toString();
    }
}