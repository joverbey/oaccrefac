
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
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsParams;

/**
 * TileLoops performs the tile loops refactoring.
 */
public class TileLoops extends CLILoopRefactoring<TileLoopsParams, TileLoopsCheck> {

    /**
     * width represents the width of the tiles.
     */
    private final int width;

    /**
     * height represents the height of the tiles
     */
    private final int height;
    
    public TileLoops(int width, int height) {
    	this.width = width;
    	this.height = height;
    }

    @Override
    protected TileLoopsCheck createCheck(IASTStatement loop) {
        return new TileLoopsCheck((IASTForStatement) loop);
    }

    @Override
    protected TileLoopsParams createParams(IASTStatement forLoop) {
        return new TileLoopsParams(width, height);
    }

    @Override
    public TileLoopsAlteration createAlteration(IASTRewrite rewriter, TileLoopsCheck check) throws CoreException {
        return new TileLoopsAlteration(rewriter, width, height, check);
    }

}
