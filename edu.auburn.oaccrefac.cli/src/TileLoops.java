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
import edu.auburn.oaccrefac.core.transformations.TileLoopsParams;
import edu.auburn.oaccrefac.core.transformations.TileLoopsCheck;
import edu.auburn.oaccrefac.core.transformations.TileLoopsAlteration;

/**
 * Command line driver to tile loops.
 */
public class TileLoops extends Main<TileLoopsParams, TileLoopsCheck, TileLoopsAlteration> {
    
    public static void main(String[] args) {
        new TileLoops().run(args);
    }

    private int width = 0;
    
    private int height = 0;
    
    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 3) {
            printUsage();
            return false;
        }
        try {
            width = Integer.parseInt(args[1]);
            height = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            printUsage();
            return false;
        }
        return true;
    }

    private void printUsage() {
        System.err.println("Usage: TileLoops <filename.c> <width> <height>");
    }

    @Override
    protected TileLoopsCheck createCheck(IASTForStatement loop) {
        return new TileLoopsCheck(loop);
    }

    @Override
    protected TileLoopsParams createParams(IASTForStatement forLoop) {
        return new TileLoopsParams(width, height);
    }

    @Override
    protected TileLoopsAlteration createAlteration(IASTRewrite rewriter, TileLoopsCheck check) throws CoreException {
        return new TileLoopsAlteration(rewriter, width, height, check);
    }
    
}
