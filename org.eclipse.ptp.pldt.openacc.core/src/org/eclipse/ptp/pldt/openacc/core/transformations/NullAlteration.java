/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a refactoring that performs
 * a dependence analysis but makes no changes to the source code.  It is intended for testing,
 * since it will refuse to refactor any code that cannot be analyzed.
 * 
 * @author Jeff Overbey
 */
public class NullAlteration extends ForLoopAlteration<NullCheck> {

    /**
     * Constructor.
     * 
     * @param rewriter
     *            -- rewriter associated with loop
     * @throws DOMException
     */
    public NullAlteration(IASTRewrite rewriter, NullCheck check) {
        super(rewriter, check);
        if (this.getLoop() == null) throw new IllegalStateException("loop is null");
    }

    @Override
    protected void doChange() {
        this.insert(0, ""); // FIXME: Should not be necessary (get NPE otherwise) //$NON-NLS-1$
        finalizeChanges();
    }
}