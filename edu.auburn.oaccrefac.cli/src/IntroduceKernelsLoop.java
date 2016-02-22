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
import edu.auburn.oaccrefac.core.transformations.IntroduceKernelsLoopCheck;
import edu.auburn.oaccrefac.core.transformations.IntroduceKernelsLoopAlteration;

/**
 * IntroduceKernelsLoop performs the introduce kernels loop refactoring.
 */
public class IntroduceKernelsLoop extends LoopMain<RefactoringParams, IntroduceKernelsLoopCheck, IntroduceKernelsLoopAlteration> {
    
    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new IntroduceKernelsLoop().run(args);
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
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: IntroduceKernelsLoop <filename.c>");
    }

    
    @Override
    protected IntroduceKernelsLoopCheck createCheck(IASTForStatement loop) {
        return new IntroduceKernelsLoopCheck(loop);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected IntroduceKernelsLoopAlteration createAlteration(IASTRewrite rewriter, IntroduceKernelsLoopCheck check) throws CoreException {
        return new IntroduceKernelsLoopAlteration(rewriter, check);
    }
    
}
