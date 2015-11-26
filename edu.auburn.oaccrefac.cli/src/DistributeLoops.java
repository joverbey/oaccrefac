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
import edu.auburn.oaccrefac.core.transformations.DistributeLoopsCheck;
import edu.auburn.oaccrefac.core.transformations.DistributeLoopsAlteration;

/**
 * DistributeLoops performs the distribute loops refactoring.
 */
public class DistributeLoops extends LoopMain<RefactoringParams, DistributeLoopsCheck, DistributeLoopsAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new IntroduceKernelsLoop().run(args);
    }

    /**
     * checkArgs checks the arguments to the refactoring.
     * 
     * @param args Arguments to the refactoring.
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
        System.err.println("Usage: DistributeLoops <filename.c>");
    }

    /**
     * createCheck creates a DistributeLoopsCheck.
     * 
     * @param loop Loop to create the check for.
     * @return Check to be performed on the loop.
     */
    @Override
    protected DistributeLoopsCheck createCheck(IASTForStatement loop) {
        return new DistributeLoopsCheck(loop);
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
     * createAlteration creates a DistributeLoopsAlteration.
     * 
     * @param reweriter Rewriter for the alteration.
     * @param check Checker for the alteration.
     * @return Alteration for the refactoring.
     * @throws CoreException if creating the alteration fails.
     */
    @Override
    protected DistributeLoopsAlteration createAlteration(IASTRewrite rewriter, DistributeLoopsCheck check) throws CoreException {
        return new DistributeLoopsAlteration(rewriter, check);
    }
    
}
