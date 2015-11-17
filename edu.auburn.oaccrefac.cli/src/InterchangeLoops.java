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
 * Command line driver to introduce a kernels loop.
 */
public class InterchangeLoops extends Main<InterchangeLoopParams, InterchangeLoopsCheck, InterchangeLoopsAlteration> {
    
    public static void main(String[] args) {
        new InterchangeLoops().run(args);
    }
    
    private int depth = 0;

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

    private void printUsage() {
        System.err.println("Usage: InterchangeLoops <filename.c> <depth>");
    }

    @Override
    protected InterchangeLoopsCheck createCheck(IASTForStatement loop) {
        return new InterchangeLoopsCheck(loop);
    }

    @Override
    protected InterchangeLoopParams createParams(IASTForStatement forLoop) {
        return new InterchangeLoopParams(depth);
    }

    @Override
    protected InterchangeLoopsAlteration createAlteration(IASTRewrite rewriter, InterchangeLoopsCheck check) throws CoreException {
        return new InterchangeLoopsAlteration(rewriter, check);
    }
    
}
