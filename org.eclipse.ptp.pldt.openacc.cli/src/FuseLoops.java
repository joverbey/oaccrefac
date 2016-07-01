
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
import org.eclipse.ptp.pldt.openacc.core.transformations.FuseLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.FuseLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;

/**
 * FuseLoops performs the fuse loops refactoring.
 */
public class FuseLoops extends CLILoopRefactoring<RefactoringParams, FuseLoopsCheck> {

    @Override
    protected FuseLoopsCheck createCheck(IASTStatement loop) {
        return new FuseLoopsCheck(new RefactoringStatus(), (IASTForStatement) loop);
    }

    @Override
    public FuseLoopsAlteration createAlteration(IASTRewrite rewriter, FuseLoopsCheck check) throws CoreException {
        return new FuseLoopsAlteration(rewriter, check);
    }

}
