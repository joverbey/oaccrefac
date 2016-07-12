
/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *     Jacob Allen Neeley (Auburn) - initial API and implementation
 *******************************************************************************/

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.UnrollLoopAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.UnrollLoopCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.UnrollLoopParams;

/**
 * Unroll performs the unroll loops refactoring.
 */
public class UnrollLoop extends CLILoopRefactoring<UnrollLoopParams, UnrollLoopCheck> {

    /**
     * unrollFactor represents how much to unroll the loop.
     */
    private final int unrollFactor;
    
    public UnrollLoop(int unrollFactor) {
    	this.unrollFactor = unrollFactor;
    }

    /**
     * createCheck creates an UnrollLoopCheck.
     * 
     * @param loop
     *            Loop to create the check for.
     * @return Check to be performed on the loop.
     */
    @Override
    protected UnrollLoopCheck createCheck(IASTStatement loop) {
        return new UnrollLoopCheck(new RefactoringStatus(), (IASTForStatement) loop);
    }

    @Override
    protected UnrollLoopParams createParams(IASTStatement forLoop) {
        return new UnrollLoopParams(unrollFactor);
    }

    @Override
    public UnrollLoopAlteration createAlteration(IASTRewrite rewriter, UnrollLoopCheck check) throws CoreException {
        return new UnrollLoopAlteration(rewriter, unrollFactor, check);
    }

}
