
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.transformations.FuseLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.FuseLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;

/**
 * FuseLoops performs the fuse loops refactoring.
 */
public class FuseLoops extends LoopMain<RefactoringParams, FuseLoopsCheck, FuseLoopsAlteration> {

    /**
     * main begins refactoring execution.
     * 
     * @param args
     *            Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new FuseLoops().run(args);
    }

    @Override
    protected boolean checkArgs(String[] args) {
        if (!((args.length == 3 && args[1].equals("-ln")) || (args.length == 1 ))) {
                printUsage();
                return false;
        }
        return true;
    }

    /**
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: FuseLoops <filename.c>");
        System.err.println("Usage2: FuseLoops <filename.c> -ln <loopname>");
    }

    @Override
    protected FuseLoopsCheck createCheck(IASTForStatement loop) {
        return new FuseLoopsCheck(loop);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected FuseLoopsAlteration createAlteration(IASTRewrite rewriter, FuseLoopsCheck check) throws CoreException {
        return new FuseLoopsAlteration(rewriter, check);
    }

}