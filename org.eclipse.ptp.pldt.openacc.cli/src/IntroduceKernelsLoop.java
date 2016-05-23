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
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroduceKernelsLoopAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroduceKernelsLoopCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;

/**
 * IntroduceKernelsLoop performs the introduce kernels loop refactoring.
 */
public class IntroduceKernelsLoop 
		extends CLILoopRefactoring<RefactoringParams, IntroduceKernelsLoopCheck, IntroduceKernelsLoopAlteration> {

    
    @Override
    protected IntroduceKernelsLoopCheck createCheck(IASTStatement loop) {
        return new IntroduceKernelsLoopCheck((IASTForStatement) loop);
    }

    @Override
    public IntroduceKernelsLoopAlteration createAlteration(IASTRewrite rewriter, IntroduceKernelsLoopCheck check) throws CoreException {
        return new IntroduceKernelsLoopAlteration(rewriter, check);
    }
    
}
