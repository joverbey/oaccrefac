/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

/**
 * This class defines the base strategy interface to be derived from for changes made to a for loop.
 * 
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopAlteration<T extends ForLoopCheck<?>> extends SourceAlteration<T> {

    private final IASTForStatement loop;
    
    /**
     * Constructor that takes a for-loop and a rewriter (for base)
     * 
     * @param rewriter
     *            -- rewriter to be given to base class
     * @param loopToChange
     *            -- loop to change
     * @throws IllegalArgumentException
     *             if the for loop is null
     */
    public ForLoopAlteration(IASTRewrite rewriter, T check) {
        super(rewriter, check);
        this.loop = check.getLoop();
    }

    /**
     * Gets the loop set from constructor
     * 
     * @return loop to change
     */
    public IASTForStatement getLoop() {
        return loop;
    }

}
