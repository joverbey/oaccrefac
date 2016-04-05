/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Jacob Neeley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

public class LoopCuttingParams extends RefactoringParams {
    private int cutFactor;
    
    public LoopCuttingParams(int stripFactor) {
        this.cutFactor = stripFactor;
    }

    public int getCutFactor() {
        return cutFactor;
    }
}