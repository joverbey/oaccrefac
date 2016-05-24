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
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroOpenACCLoopAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroOpenACCLoopCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;

/**
 * IntroduceParallelLoop performs the introduce parallel loop refactoring.
 */
public class IntroOpenACCLoop extends LoopMain<RefactoringParams, IntroOpenACCLoopCheck, IntroOpenACCLoopAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new IntroOpenACCLoop().run(args);
    }

    @Override
    protected boolean checkArgs(String[] args) {
        if (!((args.length == 3 && args[1].equals("-ln")) || (args.length == 1 ))) {
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
        System.err.println("Usage2: IntroduceParallelLoop <filename.c> -ln <loopname>");
    }

    @Override
    protected IntroOpenACCLoopCheck createCheck(IASTForStatement loop) {
        return new IntroOpenACCLoopCheck(loop);
    }
    
    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }
    
    @Override
    protected IntroOpenACCLoopAlteration createAlteration(IASTRewrite rewriter, IntroOpenACCLoopCheck check) throws CoreException {
        return new IntroOpenACCLoopAlteration(rewriter, check);
    }

}
