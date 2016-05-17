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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineParams;

/**
 * StripMine performs the strip mine refactoring.
 */
public class StripMine extends LoopMain<StripMineParams, StripMineCheck, StripMineAlteration> {

    /**
     * main begins refactoring execution.
     * 
     * @param args
     *            Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new StripMine().run(args);
    }

    /**
     * stripFactor is the factor to use in strip mining the loop
     */
    private int stripFactor = 0;
    private String newName = "";

    @Override
    protected boolean checkArgs(String[] args) {
        if (!(((args.length == 4 || args.length == 5) && args[1].equals("-ln")) || ((args.length == 3
        		|| args.length == 2)))) {
            printUsage();
            return false;
        }
        if (args[1].equals("-ln")) {
            try {
                stripFactor = Integer.parseInt(args[3]);
                if (args.length == 5) {
                	newName = args[4];
                }
            } catch (NumberFormatException e) {
                printUsage();
                return false;
            }
        } else {
            try {
                stripFactor = Integer.parseInt(args[1]);
                if (args.length == 3) {
                	newName = args[2];
                }
            } catch (NumberFormatException e) {
                printUsage();
                return false;
            }
        }
        return true;
    }

    /**
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: StripMine <filename.c> <strip_factor>");
        System.err.println("Usage: StripMine <filename.c> -ln <loopname> <strip_factor>");
    }

    @Override
    protected StripMineCheck createCheck(IASTForStatement loop) {
        return new StripMineCheck(loop);
    }

    @Override
    protected StripMineParams createParams(IASTForStatement forLoop) {
        return new StripMineParams(stripFactor, newName);
    }

    @Override
    protected StripMineAlteration createAlteration(IASTRewrite rewriter, StripMineCheck check) throws CoreException {
        return new StripMineAlteration(rewriter, stripFactor, newName, check);
    }

}
