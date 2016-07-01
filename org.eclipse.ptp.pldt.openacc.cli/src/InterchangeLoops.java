
/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
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
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.InterchangeLoopParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.InterchangeLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.InterchangeLoopsCheck;

/**
 * InterchangeLoops performs the interchange loops alteration.
 */
public class InterchangeLoops extends CLILoopRefactoring<InterchangeLoopParams, InterchangeLoopsCheck> {

    /**
     * depth is the depth to interchange to.
     */
    private int depth;
    
    public InterchangeLoops(int depth) {
    	this.depth = depth;
    }

    @Override
    protected InterchangeLoopsCheck createCheck(IASTStatement loop) {
        return new InterchangeLoopsCheck(new RefactoringStatus(), (IASTForStatement) loop);
    }

    @Override
    protected InterchangeLoopParams createParams(IASTStatement forLoop) {
        return new InterchangeLoopParams(depth);
    }

    @Override
	public InterchangeLoopsAlteration createAlteration(IASTRewrite rewriter, InterchangeLoopsCheck check)
            throws CoreException {
        return new InterchangeLoopsAlteration(rewriter, check);
    }

}
