/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.transformations.AbstractTileLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.AbstractTileLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.AbstractTileLoopsParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineParams;

/**
 * StripMine performs the strip mine refactoring.
 */
public class StripMineLoop extends CLILoopRefactoring<AbstractTileLoopsParams, AbstractTileLoopsCheck> {

	private final boolean cut;

    /**
     * stripFactor is the factor to use in strip mining the loop
     */
    private final int numFactor;
    private final String newName;
    
    public StripMineLoop(int numFactor, String newName, boolean cut) {
    	this.numFactor = numFactor;
    	this.newName = newName;
    	this.cut = cut;
    }

    @Override
    public AbstractTileLoopsCheck createCheck(IASTStatement loop) {
    	if (cut) {
    		return new LoopCuttingCheck((IASTForStatement) loop);
    	} else {
    		return new StripMineCheck((IASTForStatement) loop);
    	}
    }

    @Override
    protected AbstractTileLoopsParams createParams(IASTStatement forLoop) {
    	if (cut) {
    		return new LoopCuttingParams(numFactor, newName);
    	} else {
            return new StripMineParams(numFactor, newName);
    	}
    }

    @Override
	public AbstractTileLoopsAlteration createAlteration(IASTRewrite rewriter, 
    		AbstractTileLoopsCheck check) throws CoreException {
    	if (cut) {
    		return new LoopCuttingAlteration(rewriter, numFactor, newName, check);
    	} else {
            return new StripMineAlteration(rewriter, numFactor, newName, check);
    	}
    }

}
