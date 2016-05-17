/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

public class StripMineParams extends RefactoringParams {

    private int stripFactor;
    private String newName;

    public StripMineParams(int stripFactor, String newName) {
        this.stripFactor = stripFactor;
        this.newName = newName;
    }

    public int getStripFactor() {
        return stripFactor;
    }
    
    public String getNewName() {
    	return newName;
    }

}