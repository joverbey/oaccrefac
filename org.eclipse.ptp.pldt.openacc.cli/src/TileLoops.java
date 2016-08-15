/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *     Jacob Allen Neeley (Auburn) - initial API and implementation
 *******************************************************************************/

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.AbstractTileLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.AbstractTileLoopsCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.AbstractTileLoopsParams;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StridedTileAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StridedTileCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StridedTileParams;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.TileLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.TileLoopsCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.TileLoopsParams;

/**
 * TileLoops performs the tile loops refactoring.
 */
public class TileLoops extends CLILoopRefactoring<AbstractTileLoopsParams, AbstractTileLoopsCheck> {

    /**
     * width represents the width of the tiles.
     */
    private final int width;

    /**
     * height represents the height of the tiles
     */
    private final int height;
    
    /**
     * names for the index variables created
     */
    private final String name1, name2;
    
    /**
     * whether to perform strided tiling
     */
    private final boolean strided;
    
    public TileLoops(int width, int height, String name1, String name2, boolean strided) {
    	this.width = width;
    	this.height = height;
    	this.name1 = name1;
    	this.name2 = name2;
    	this.strided = strided;
    }

    @Override
    protected AbstractTileLoopsCheck createCheck(IASTStatement loop) {
    	if (strided) {
    		return new StridedTileCheck(new RefactoringStatus(), (IASTForStatement) loop);
    	} else {
    		return new TileLoopsCheck(new RefactoringStatus(), (IASTForStatement) loop);
    	}
    }

    @Override
    protected AbstractTileLoopsParams createParams(IASTStatement forLoop) {
    	if (strided) {
    		return new StridedTileParams(width, name1);
    	} else {
            return new TileLoopsParams(width, height);
    	}
    }

    @Override
    public AbstractTileLoopsAlteration createAlteration(IASTRewrite rewriter, 
    		AbstractTileLoopsCheck check) throws CoreException {
    	if (strided) {
    		return new StridedTileAlteration(rewriter, width, name1, check);
    	} else {
    		return new TileLoopsAlteration(rewriter, width, height, name1, name2, (TileLoopsCheck) check);
    	}
    }
}
