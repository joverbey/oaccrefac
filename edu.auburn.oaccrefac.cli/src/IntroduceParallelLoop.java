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

    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 2) {
            printUsage();
            return false;
        }
        return true;
    }
    
    /**
     * <filename.c> 
     * If arg is only file name and no refactoring, take this branch
     * Define intermediate methods to find all loop statements
     * When you find the loop statements
     * Refactoring args to the refactoring
     */

    /**
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: IntroduceParallelLoop <filename.c>");
    }

    @Override
    protected IntroParallelCheck createCheck(IASTForStatement loop) {
        return new IntroParallelCheck(loop);
    }
    
    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }
    
    @Override
    protected IntroParallelAlteration createAlteration(IASTRewrite rewriter, IntroParallelCheck check) throws CoreException {
        return new IntroParallelAlteration(rewriter, check);
    }
    
}