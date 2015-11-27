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
import edu.auburn.oaccrefac.core.transformations.StripMineParams;
import edu.auburn.oaccrefac.core.transformations.StripMineCheck;
import edu.auburn.oaccrefac.core.transformations.StripMineAlteration;

/**
 * StripMine performs the strip mine refactoring.
 */
public class StripMine extends LoopMain<StripMineParams, StripMineCheck, StripMineAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new StripMine().run(args);
    }

    /**
     * stripFactor is the factor to use in strip mining the loop
     */
    private int stripFactor = 0;
    
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
            stripFactor = Integer.parseInt(args[1]);
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
        System.err.println("Usage: StripMine <filename.c> <strip_factor>");
    }

    /**
     * createCheck creates a StripMineCheck.
     * 
     * @param loop Loop to create the check for.
     * @return Check to be performed on the loop.
     */
    @Override
    protected StripMineCheck createCheck(IASTForStatement loop) {
        return new StripMineCheck(loop);
    }

    /**
     * createParams creates StripMineParams.
     * 
     * @param forLoop Not used.
     * @return StripMineParams made with the stripFactor.
     */
    @Override
    protected StripMineParams createParams(IASTForStatement forLoop) {
        return new StripMineParams(stripFactor);
    }

    /**
     * createAlteration creates a StripMineAlteration.
     * 
     * @param reweriter Rewriter for the alteration.
     * @param check Checker for the alteration.
     * @return Alteration for the refactoring.
     * @throws CoreException if creating the alteration fails.
     */
    @Override
    protected StripMineAlteration createAlteration(IASTRewrite rewriter, StripMineCheck check) throws CoreException {
        return new StripMineAlteration(rewriter, stripFactor, check);
    }
    
}
