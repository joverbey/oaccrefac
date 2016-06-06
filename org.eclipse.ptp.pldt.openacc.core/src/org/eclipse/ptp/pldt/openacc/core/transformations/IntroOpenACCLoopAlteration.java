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

public class IntroOpenACCLoopAlteration extends ForLoopAlteration<IntroOpenACCLoopCheck> {

	private boolean kernels;
	private boolean inParallelRegion;
	
    public IntroOpenACCLoopAlteration(IASTRewrite rewriter, IntroOpenACCLoopCheck check, boolean kernels) {
        super(rewriter, check);
        this.kernels = kernels;
        this.inParallelRegion = check.isInParallelRegion();
    }

    @Override
    protected void doChange() {
    	if(inParallelRegion){
    		int offset = getLoop().getFileLocation().getNodeOffset();
    		this.insert(offset, pragma("acc loop") + System.lineSeparator());
    		finalizeChanges();
    	} else if (kernels) {
    		int offset = getLoop().getFileLocation().getNodeOffset();
            this.insert(offset, pragma("acc kernels loop"));
            finalizeChanges();
    	} else {
    		int offset = getLoop().getFileLocation().getNodeOffset();
    		this.insert(offset, pragma("acc parallel loop") + System.lineSeparator());
    		finalizeChanges();
    	}
    }

}
