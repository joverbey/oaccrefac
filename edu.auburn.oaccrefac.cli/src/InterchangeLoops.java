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
import edu.auburn.oaccrefac.core.transformations.InterchangeLoopParams;
import edu.auburn.oaccrefac.core.transformations.InterchangeLoopsCheck;
import edu.auburn.oaccrefac.core.transformations.InterchangeLoopsAlteration;

/**
 * InterchangeLoops performs the interchange loops alteration.
 */
public class InterchangeLoops extends LoopMain<InterchangeLoopParams, InterchangeLoopsCheck, InterchangeLoopsAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new InterchangeLoops().run(args);
    }
    
    /**
     * depth is the depth to interchange to.
     */
    private int depth = 0;

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
            depth = Integer.parseInt(args[1]);
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
        System.err.println("Usage: InterchangeLoops <filename.c> <depth>");
    }

    /**
     * createCheck creates an InterchangeLoopsCheck.
     * 
     * @param loop Loop to create the check for.
     * @return Check to be performed on the loop.
     */
    @Override
    protected InterchangeLoopsCheck createCheck(IASTForStatement loop) {
        return new InterchangeLoopsCheck(loop);
    }

    /**
     * createParams creates InterchangeLoopParams.
     * 
     * @param forLoop Loop to create the parameters for.
     * @return Parameters for the refactoring.
     */
    @Override
    protected InterchangeLoopParams createParams(IASTForStatement forLoop) {
        return new InterchangeLoopParams(depth);
    }

    /**
     * createAlteration creates an InterchangeLoopsAlteration.
     * 
     * @param rewriter Rewriter for the alteration.
     * @param check Checker for the alteration.
     * @return Alteration for the refactoring.
     * @throws CoreException If creating the alteration fails.
     */
    @Override
    protected InterchangeLoopsAlteration createAlteration(IASTRewrite rewriter, InterchangeLoopsCheck check) throws CoreException {
        return new InterchangeLoopsAlteration(rewriter, check);
    }
    
}
