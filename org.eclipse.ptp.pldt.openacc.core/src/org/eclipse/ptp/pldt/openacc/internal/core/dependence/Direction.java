/*******************************************************************************
 * Copyright (c) 2009, 2016 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.dependence;

/**
 * Enumeration representing the possible entries in a direction vector:
 * &quot;&lt;&quot, &quot;=&quot;, &quot;&gt;&quot;, &quot;&le;&quot;, or &quot;&ge;&quot;.
 * <p>
 * A special value &quot;*&quot, used to indicate &quot;any direction&quot;
 * in the Banerjee Inequality, is also provided.
 * <p>
 * Reference: Allen and Kennedy, <i>Optimizing Compilers for Modern
 * Architectures.</i>  Direction vectors are defined on p. 46
 * (Definition 2.10); the Banerjee Inequality is described on pp. 97-111.
 * <p>
 * 
 * @author Adam Eichelkraut
 * @author Alexander Calvert
 * @author Jeff Overbey
 * @see IDependenceTester
 */
public enum Direction {
    GT(">"), LT("<"), EQ("="), GE(">="), LE("<="), ANY("*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

    private final String string;

    private Direction(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return this.string;
    }
}