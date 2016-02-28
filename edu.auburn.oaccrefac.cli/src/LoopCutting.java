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
        new LoopCutting().run(args);
    }

    /**
     * cutFactor represents how much the loop is cut.
     */
    private int cutFactor = 0;
    
    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 3) {
            printUsage();
            return false;
        }
        
        try {
            cutFactor = Integer.parseInt(args[2]);
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

    @Override
    protected LoopCuttingCheck createCheck(IASTForStatement loop) {
        return new LoopCuttingCheck(loop);
    }

    @Override
    protected LoopCuttingParams createParams(IASTForStatement forLoop) {
        return new LoopCuttingParams(cutFactor);
    }

    @Override
    protected LoopCuttingAlteration createAlteration(IASTRewrite rewriter, LoopCuttingCheck check) throws CoreException {
        return new LoopCuttingAlteration(rewriter, cutFactor, check);
    }
    
}
