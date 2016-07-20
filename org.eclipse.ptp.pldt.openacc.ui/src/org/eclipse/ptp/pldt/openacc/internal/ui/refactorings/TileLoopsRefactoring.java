/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.AbstractTileLoopsCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StridedTileAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StridedTileCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StridedTileParams;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.TileLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.TileLoopsCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.TileLoopsParams;

/**
 * "The basic algorithm for blocking (tiling) is called strip-mine-and-interchange. Basically, it consists of
 * strip-mining a given loop into two loops, one that iterates within contiguous strips and an outer loop that iterates
 * strip-by-strip, then interchanging the by-strip loop to the outside of the outer containing loops." -- 9.3.2 Legality
 * of Blocking, p.480, Optimizing Compilers for Modern Architectures
 * 
 */
public class TileLoopsRefactoring extends ForLoopRefactoring {

	private boolean cut = false;
    private int width = 0;
    private int height = 0;
    private int cutFactor = 0;
    private String newName = ""; //$NON-NLS-1$
    private String innerNewName = ""; //$NON-NLS-1$
    private String outerNewName = ""; //$NON-NLS-1$
    private AbstractTileLoopsCheck check;

    public TileLoopsRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    
    public void setNewName(String newName) {
    	this.newName = newName;
    }
    
    public void setInnerNewName(String innerNewName) {
    	this.innerNewName = innerNewName;
    }
    
    public void setOuterNewName(String outerNewName) {
    	this.outerNewName = outerNewName;
    }
    
    public void setStride(int cutFactor) {
    	this.cutFactor = cutFactor;
    }
    
    public void setCut(boolean cut) {
    	this.cut = cut;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
    	if (cut) {
    		check = new StridedTileCheck(status, getLoop());
    		check.performChecks(pm, new StridedTileParams(cutFactor, newName));
    	} else {
	        check = new TileLoopsCheck(status, getLoop());
	        check.performChecks(pm, new TileLoopsParams(width, height));
    	}
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
    	if (cut) {
    		new StridedTileAlteration(rewriter, cutFactor, newName, check).change();
    	} else {
    		new TileLoopsAlteration(rewriter, width, height, innerNewName,
    				outerNewName, (TileLoopsCheck) check).change();
    	}
    }
}
