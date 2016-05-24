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
import org.eclipse.ptp.pldt.openacc.core.transformations.DistributeLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.DistributeLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;

/**
 * DistributeLoops performs the distribute loops refactoring.
 */
public class DistributeLoops
		extends CLILoopRefactoring<RefactoringParams, DistributeLoopsCheck> {

    @Override
    protected DistributeLoopsCheck createCheck(IASTStatement loop) {
        return new DistributeLoopsCheck((IASTForStatement) loop);
    }

    @Override
    public DistributeLoopsAlteration createAlteration(IASTRewrite rewriter, DistributeLoopsCheck check)
            throws CoreException {
        return new DistributeLoopsAlteration(rewriter, check);
    }

}
