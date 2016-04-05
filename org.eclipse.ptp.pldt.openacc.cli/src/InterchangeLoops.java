
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
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.InterchangeLoopParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.InterchangeLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.InterchangeLoopsCheck;

/**
 * InterchangeLoops performs the interchange loops alteration.
 */
public class InterchangeLoops
        extends LoopMain<InterchangeLoopParams, InterchangeLoopsCheck, InterchangeLoopsAlteration> {

    /**
     * main begins refactoring execution.
     * 
     * @param args
     *            Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new InterchangeLoops().run(args);
    }

    /**
     * depth is the depth to interchange to.
     */
    private int depth = 0;

    @Override
    protected boolean checkArgs(String[] args) {
        if (!((args.length == 4 && args[1].equals("-ln")) || (args.length == 2 ))) {
                printUsage();
                return false;
        }
        if (args[1].equals("-ln")) {
            try {
                depth = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                printUsage();
                return false;
            }
        } 
        else {
            try {
                depth = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                printUsage();
                System.err.println("test");
                return false;
            }
        }

        return true;
    }

    /**
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: InterchangeLoops <filename.c> <depth>");
        System.err.println("Usage2: InterchangeLoops <filename.c> -ln <loopname> <depth>");
    }

    @Override
    protected InterchangeLoopsCheck createCheck(IASTForStatement loop) {
        return new InterchangeLoopsCheck(loop);
    }

    @Override
    protected InterchangeLoopParams createParams(IASTForStatement forLoop) {
        return new InterchangeLoopParams(depth);
    }

    @Override
    protected InterchangeLoopsAlteration createAlteration(IASTRewrite rewriter, InterchangeLoopsCheck check)
            throws CoreException {
        return new InterchangeLoopsAlteration(rewriter, check);
    }

}
