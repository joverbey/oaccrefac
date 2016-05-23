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
import org.eclipse.ptp.pldt.openacc.core.transformations.AbstractStripMineAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.AbstractStripMineCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.AbstractStripMineParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineParams;

/**
 * StripMine performs the strip mine refactoring.
 */
public class StripMineLoop extends LoopMain<AbstractStripMineParams, AbstractStripMineCheck, AbstractStripMineAlteration> {

	private boolean cut = false;
	
    /**
     * main begins refactoring execution.
     * 
     * @param args
     *            Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new StripMineLoop().run(args);
    }

    /**
     * stripFactor is the factor to use in strip mining the loop
     */
    private int numFactor = 0;
    private String newName = "";

    @Override
    protected boolean checkArgs(String[] args) {
    	if (args.length < 2) {
            printUsage();
            return false;
        }
        if (args[1].equals("-ln")) {
        	if (args[3].equals("-c")) {
        		cut = true;
        		if (args.length != 5 && args.length != 6) {
        			printUsage();
        			return false;
        		}
        		try {
        			numFactor = Integer.parseInt(args[4]);
        			if (args.length == 6) {
        				newName = args[5];
        			}
        		} catch (NumberFormatException e) {
        			printUsage();
        			return false;
        		}
        	} else {
	        	if (args.length != 4 && args.length != 5) {
	                printUsage();
	                return false;
	            }
	            try {
	                numFactor = Integer.parseInt(args[3]);
	                if (args.length == 5) {
	                	newName = args[4];
	                }
	            } catch (NumberFormatException e) {
	                printUsage();
	                return false;
	            }
        	}
        } else if (args[1].equals("-c")) {
        	cut = true;
        	if (args.length != 3 && args.length != 4) {
                printUsage();
                return false;
            }
            try {
                numFactor = Integer.parseInt(args[2]);
                if (args.length == 4) {
                	newName = args[3];
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
                numFactor = Integer.parseInt(args[1]);
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
        System.err.println("Usage: StripMine <filename.c> <strip_factor> <index_name>");
        System.err.println("Usage: StripMine <filename.c> -c <strip_factor> <index_name>");
        System.err.println("Usage: StripMine <filename.c> -ln <loopname> <strip_factor> <index_name>");
        System.err.println("Usage: StripMine <filename.c> -ln <loopname> -c <strip_factor> <index_name>");
    }

    @Override
    protected AbstractStripMineCheck createCheck(IASTForStatement loop) {
    	if (cut) {
    		return new LoopCuttingCheck(loop);
    	} else {
    		return new StripMineCheck(loop);
    	}
    }

    @Override
    protected AbstractStripMineParams createParams(IASTForStatement forLoop) {
    	if (cut) {
    		return new LoopCuttingParams(numFactor, newName);
    	} else {
            return new StripMineParams(numFactor, newName);
    	}
    }

    @Override
    protected AbstractStripMineAlteration createAlteration(IASTRewrite rewriter, 
    		AbstractStripMineCheck check) throws CoreException {
    	if (cut) {
    		return new LoopCuttingAlteration(rewriter, numFactor, newName, check);
    	} else {
            return new StripMineAlteration(rewriter, numFactor, newName, check);
    	}
    }

}
