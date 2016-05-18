
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
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingParams;

/**
 * LoopCutting performs the loop cutting refactoring.
 */
public class LoopCutting extends LoopMain<LoopCuttingParams, LoopCuttingCheck, LoopCuttingAlteration> {

    /**
     * main begins refactoring execution.
     * 
     * @param args
     *            Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new LoopCutting().run(args);
    }

    /**
     * cutFactor represents how much the loop is cut.
     */
    private int cutFactor = 0;
    private String newName = "";

    @Override
    protected boolean checkArgs(String[] args) {
    	if (args.length < 2) {
            printUsage();
            return false;
        }
        if (args[1].equals("-ln")) {
        	if (args.length != 4 && args.length != 5) {
                printUsage();
                return false;
            }
            try {
                cutFactor = Integer.parseInt(args[3]);
                if (args.length == 5) {
                	newName = args[4];
                }
            } catch (NumberFormatException e) {
                printUsage();
                return false;
            }
        } else {
        	if (args.length != 2 && args.length != 3) {
                printUsage();
                return false;
            }
            try {
                cutFactor = Integer.parseInt(args[1]);
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
        System.err.println("Usage: LoopCutting <filename.c> <cut_factor>");
        System.err.println("Usage: LoopCutting <filename.c> -ln <loopname> <cut_factor>");
    }

    @Override
    protected LoopCuttingCheck createCheck(IASTForStatement loop) {
        return new LoopCuttingCheck(loop);
    }

    @Override
    protected LoopCuttingParams createParams(IASTForStatement forLoop) {
        return new LoopCuttingParams(cutFactor, newName);
    }

    @Override
    protected LoopCuttingAlteration createAlteration(IASTRewrite rewriter, LoopCuttingCheck check)
            throws CoreException {
        return new LoopCuttingAlteration(rewriter, cutFactor, newName, check);
    }

}
