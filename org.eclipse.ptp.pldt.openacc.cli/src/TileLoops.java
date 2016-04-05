
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
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsParams;

/**
 * TileLoops performs the tile loops refactoring.
 */
public class TileLoops extends LoopMain<TileLoopsParams, TileLoopsCheck, TileLoopsAlteration> {

    /**
     * main begins refactoring execution.
     * 
     * @param args
     *            Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new TileLoops().run(args);
    }

    /**
     * width represents the width of the tiles.
     */
    private int width = 0;

    /**
     * height represents the height of the tiles
     */
    private int height = 0;

    @Override
    protected boolean checkArgs(String[] args) {
        if (!((args.length == 5 && args[1].equals("-ln")) || (args.length == 3 ))) {
            printUsage();
            return false;
        }
        if (args[1].equals("-ln")) {
            try {
                width = Integer.parseInt(args[3]);
                height = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                printUsage();
                return false;
            }
        } else {
            try {
                width = Integer.parseInt(args[1]);
                height = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                printUsage();
                return false;
            }
        }
        return true;
    }

    /**
     * printUsage prints the usage of the refactoring.
     */
    private void printUsage() {
        System.err.println("Usage: TileLoops <filename.c> <width> <height>");
        System.err.println("Usage: TileLoops <filename.c> -ln <loopname> <width> <height>");
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
