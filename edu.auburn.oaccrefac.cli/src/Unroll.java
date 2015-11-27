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
import edu.auburn.oaccrefac.core.transformations.UnrollLoopAlteration;
import edu.auburn.oaccrefac.core.transformations.UnrollLoopCheck;
import edu.auburn.oaccrefac.core.transformations.UnrollLoopParams;

/**
 * Unroll performs the unroll loops refactoring.
 */
public class Unroll extends LoopMain<UnrollLoopParams, UnrollLoopCheck, UnrollLoopAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new Unroll().run(args);
    }

    /**
     * unrollFactor represents how much to unroll the loop.
     */
    private int unrollFactor = 0;

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
            unrollFactor = Integer.parseInt(args[1]);
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
        System.err.println("Usage: Unroll <filename.c> <factor>");
    }

    /**
     * createCheck creates an UnrollLoopCheck.
     * 
     * @param loop Loop to create the check for.
     * @return Check to be performed on the loop.
     */
    @Override
    protected UnrollLoopCheck createCheck(IASTForStatement loop) {
        return new UnrollLoopCheck(loop);
    }

    /**
     * createParams creates UnrollLoopParams.
     * 
     * @param forLoop Not used.
     * @return UnrollLoopParams made with the unrollFactor.
     */
    @Override
    protected UnrollLoopParams createParams(IASTForStatement forLoop) {
        return new UnrollLoopParams(unrollFactor);
    }

    /**
     * createAlteration creates an UnrollLoopAlteration.
     * 
     * @param reweriter Rewriter for the alteration.
     * @param check Checker for the alteration.
     * @return Alteration for the refactoring.
     * @throws CoreException if creating the alteration fails.
     */
    @Override
    protected UnrollLoopAlteration createAlteration(IASTRewrite rewriter, UnrollLoopCheck check) throws CoreException {
        return new UnrollLoopAlteration(rewriter, unrollFactor, check);
    }
    
}
