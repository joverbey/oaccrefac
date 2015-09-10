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
package edu.auburn.oaccrefac.core.transformations;

public class UnrollLoopParams extends RefactoringParams {
    private int unrollFactor;

    public UnrollLoopParams(int unrollFactor) {
        this.unrollFactor = unrollFactor;
    }

    public int getUnrollFactor() {
        return unrollFactor;
    }

}