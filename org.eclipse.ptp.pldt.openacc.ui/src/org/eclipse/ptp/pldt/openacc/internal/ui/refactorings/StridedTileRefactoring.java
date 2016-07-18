/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.StridedTileAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.StridedTileCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.StridedTileParams;

public class StridedTileRefactoring extends ForLoopRefactoring{

    private int cutFactor;
    private String newName = ""; //$NON-NLS-1$
    private StridedTileCheck check;
    
    // TODO: put a good comment here
    public StridedTileRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    public void setCutFactor(int factor) {
        this.cutFactor = factor;
    }
    
    public void setNewName(String newName) {
    	this.newName = newName;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new StridedTileCheck(status, getLoop());
        check.performChecks(pm, new StridedTileParams(cutFactor, newName));
    }
    
    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new StridedTileAlteration(rewriter, cutFactor, newName, check).change();
    }
}
