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

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingParams;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingCheck;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingAlteration;

/**
 * LoopCutting performs the loop cutting refactoring.
 */
public class LoopCutting extends LoopMain<LoopCuttingParams, LoopCuttingCheck, LoopCuttingAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new IntroduceKernelsLoop().run(args);
    }

    /**
     * cutFactor represents how much the loop is cut.
     */
    private int cutFactor = 0;
    
    /**
     * checkArgs checks the arguments to the refactoring.
     * 
     * @param args Arguments to the refactoring.
     * @return Value representing the result of checking the arguments.
     */
    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 2) {
            printUsage();
            return false;
        }
        
        try {
            cutFactor = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            printUsage();
            return false;
        }
        
        return true;
    }

    /**
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: LoopCutting <filename.c> <cut_factor>");
    }

    /**
     * createCheck creates a LoopCuttingCheck.
     * 
     * @param loop Loop to create the check for.
     * @return Check to be performed on the loop.
     */
    @Override
    protected LoopCuttingCheck createCheck(IASTForStatement loop) {
        return new LoopCuttingCheck(loop);
    }

    /**
     * createParams creates LoopCuttingparams.
     * 
     * @param forLoop Not used.
     * @return LoopCuttingParams made with the cutFactor.
     */
    @Override
    protected LoopCuttingParams createParams(IASTForStatement forLoop) {
        return new LoopCuttingParams(cutFactor);
    }

    /**
     * createAlteration creates a LoopCuttingAlteration.
     * 
     * @param reweriter Rewriter for the alteration.
     * @param check Checker for the alteration.
     * @return Alteration for the refactoring.
     * @throws CoreException if creating the alteration fails.
     */
    @Override
    protected LoopCuttingAlteration createAlteration(IASTRewrite rewriter, LoopCuttingCheck check) throws CoreException {
        return new LoopCuttingAlteration(rewriter, cutFactor, check);
    }
    
}
