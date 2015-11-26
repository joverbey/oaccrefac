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

public class Unroll extends LoopMain<UnrollLoopParams, UnrollLoopCheck, UnrollLoopAlteration> {
    
    public static void main(String[] args) {
        new Unroll().run(args);
    }

    private int unrollFactor = 0;

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

    private void printUsage() {
        System.err.println("Usage: Unroll <filename.c> <factor>");
    }

    @Override
    protected UnrollLoopCheck createCheck(IASTForStatement loop) {
        return new UnrollLoopCheck(loop);
    }

    @Override
    protected UnrollLoopParams createParams(IASTForStatement forLoop) {
        return new UnrollLoopParams(unrollFactor);
    }

    @Override
    protected UnrollLoopAlteration createAlteration(IASTRewrite rewriter, UnrollLoopCheck check) throws CoreException {
        return new UnrollLoopAlteration(rewriter, unrollFactor, check);
    }
    
}
