
/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingParams;

/**
 * LoopCutting performs the loop cutting refactoring.
 */
public class LoopCutting extends CLILoopRefactoring<LoopCuttingParams, LoopCuttingCheck, LoopCuttingAlteration> {

    /**
     * cutFactor represents how much the loop is cut.
     */
    private int cutFactor = 0;
    
    public LoopCutting(int cutFactor) {
    	this.cutFactor = cutFactor;
    }

    @Override
    protected LoopCuttingCheck createCheck(IASTStatement loop) {
        return new LoopCuttingCheck((IASTForStatement) loop);
    }

    @Override
    protected LoopCuttingParams createParams(IASTStatement forLoop) {
        return new LoopCuttingParams(cutFactor);
    }

    @Override
    public LoopCuttingAlteration createAlteration(IASTRewrite rewriter, LoopCuttingCheck check)
            throws CoreException {
        return new LoopCuttingAlteration(rewriter, cutFactor, check);
    }

}
