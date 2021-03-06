/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

public abstract class AbstractTileLoopsParams extends RefactoringParams {
    protected int numFactor;
    private String newName;
    
    public AbstractTileLoopsParams(int numValue, String newName) {
        this.numFactor = numValue;
        this.newName = newName;
    }
   
    public String getNewName() {
    	return newName;
    }
    
    public int getNumFactor() {
    	return numFactor;
    }
}