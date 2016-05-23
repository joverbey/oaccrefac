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
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineParams;

/**
 * StripMine performs the strip mine refactoring.
 */
public class StripMineLoop extends CLILoopRefactoring<StripMineParams, StripMineCheck, StripMineAlteration> {
    /**
     * stripFactor is the factor to use in strip mining the loop
     */
    private final int stripFactor;
    private String newName = "";
    
    public StripMineLoop(int stripFactor, String newName) {
    	this.stripFactor = stripFactor;
    	this.newName = newName;
    }

    @Override
    protected StripMineCheck createCheck(IASTStatement loop) {
        return new StripMineCheck((IASTForStatement) loop);
    }

    @Override
    protected StripMineParams createParams(IASTStatement forLoop) {
        return new StripMineParams(stripFactor, newName);
    }

    @Override
    public StripMineAlteration createAlteration(IASTRewrite rewriter, StripMineCheck check) throws CoreException {
        return new StripMineAlteration(rewriter, stripFactor, newName, check);
    }

}
