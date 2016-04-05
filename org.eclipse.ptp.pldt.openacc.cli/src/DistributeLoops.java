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
import org.eclipse.ptp.pldt.openacc.core.transformations.DistributeLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.DistributeLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.NullParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;

/**
 * DistributeLoops performs the distribute loops refactoring.
 */
public class DistributeLoops extends LoopMain<RefactoringParams, DistributeLoopsCheck, DistributeLoopsAlteration> {

    /**
     * main begins refactoring execution.
     * 
     * @param args
     *            Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new DistributeLoops().run(args);
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
        System.err.println("Usage: DistributeLoops <filename.c>");
        System.err.println("Usage2: DistributeLoops <filename.c> -ln <loopname>");
    }

    @Override
    protected DistributeLoopsCheck createCheck(IASTForStatement loop) {
        return new DistributeLoopsCheck(loop);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return new NullParams();
    }

    @Override
    protected DistributeLoopsAlteration createAlteration(IASTRewrite rewriter, DistributeLoopsCheck check)
            throws CoreException {
        return new DistributeLoopsAlteration(rewriter, check);
    }

}
