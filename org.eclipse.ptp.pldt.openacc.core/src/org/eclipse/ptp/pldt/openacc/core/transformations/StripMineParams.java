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
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

public class StripMineParams extends RefactoringParams {

	private int stripFactor;
	private boolean zeroBased;
	private boolean handleOverflow;
    private String newNameOuter;
    private String newNameInner;
    
    public StripMineParams(int stripFactor, boolean zeroBased, boolean handleOverflow, String newNameOuter, String newNameInner) {
		this.stripFactor = stripFactor;
		this.zeroBased = zeroBased;
		this.handleOverflow = handleOverflow;
		this.newNameOuter = newNameOuter;
		this.newNameInner = newNameInner;
	}

	public int getStripFactor() {
		return stripFactor;
	}

	public boolean shouldBeZeroBased() {
		return zeroBased;
	}

	public boolean shouldHandleOverflow() {
		return handleOverflow;
	}

	public String getNewNameOuter() {
		return newNameOuter;
	}

	public String getNewNameInner() {
		return newNameInner;
	}

    
    
}