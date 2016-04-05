/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.TileLoopsParams;

/**
 * "The basic algorithm for blocking (tiling) is called strip-mine-and-interchange. Basically, it consists of
 * strip-mining a given loop into two loops, one that iterates within contiguous strips and an outer loop that iterates
 * strip-by-strip, then interchanging the by-strip loop to the outside of the outer containing loops." -- 9.3.2 Legality
 * of Blocking, p.480, Optimizing Compilers for Modern Architectures
 * 
 */
public class LoopTilingRefactoring extends ForLoopRefactoring {

    private int width = 0;
    private int height = 0;
    private TileLoopsCheck check;

    public LoopTilingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new TileLoopsCheck(getLoop());
        check.performChecks(status, pm, new TileLoopsParams(width, height));
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new TileLoopsAlteration(rewriter, width, height, check).change();
    }
}