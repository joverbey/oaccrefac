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
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroOpenACCLoopAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroOpenACCLoopCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;

/**
 * IntroduceParallelLoop performs the introduce parallel loop refactoring.
 */
public class IntroOpenACCLoop extends CLILoopRefactoring<RefactoringParams, IntroOpenACCLoopCheck> {

	private boolean kernels;
	
	public IntroOpenACCLoop(boolean kernels) {
		this.kernels = kernels;
	}
    @Override
    protected IntroOpenACCLoopCheck createCheck(IASTStatement loop) {
        return new IntroOpenACCLoopCheck(new RefactoringStatus(), (IASTForStatement) loop, kernels);
    }
    
    @Override
    public IntroOpenACCLoopAlteration createAlteration(IASTRewrite rewriter, IntroOpenACCLoopCheck check)
    		throws CoreException {
        return new IntroOpenACCLoopAlteration(rewriter, check, kernels);
    }

}
