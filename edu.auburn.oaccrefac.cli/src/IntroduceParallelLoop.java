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
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.IntroParallelCheck;
import edu.auburn.oaccrefac.core.transformations.IntroParallelAlteration;

/**
 * IntroduceParallelLoop performs the introduce parallel loop refactoring.
 */
public class IntroduceParallelLoop extends LoopMain<RefactoringParams, IntroParallelCheck, IntroParallelAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new IntroduceParallelLoop().run(args);
    }

    /**
     * checkArgs checks the arguments to the refactoring.
     * 
     * @param args Arguments to the refactoring.
     * @return Value representing the result of checking the arguments.
     */
    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 1) {
            printUsage();
            return false;
        }
        return true;
    }

    /**
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: IntroduceParallelLoop <filename.c>");
    }

    /**
     * createCheck creates an IntroParallelCheck.
     * 
     * @param loop Loop to create the check for.
     * @return Check to be performed on the loop.
     */
    @Override
    protected IntroParallelCheck createCheck(IASTForStatement loop) {
        return new IntroParallelCheck(loop);
    }

    /**
     * createParams creates generic RefactoringParams.
     * 
     * @param forLoop Not used.
     * @return null because parameters are not used in this refactoring.
     */
    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    /**
     * createAlteration creates an IntroParallelAlteration.
     * 
     * @param reweriter Rewriter for the alteration.
     * @param check Checker for the alteration.
     * @return Alteration for the refactoring.
     * @throws CoreException if creating the alteration fails.
     */
    @Override
    protected IntroParallelAlteration createAlteration(IASTRewrite rewriter, IntroParallelCheck check) throws CoreException {
        return new IntroParallelAlteration(rewriter, check);
    }
    
}
