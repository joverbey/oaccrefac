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
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.LoopCuttingParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineParams;

public class StripMineLoopRefactoring extends ForLoopRefactoring {

    private int numFactor = -1;
    private String newName = "";
    private StripMineCheck stripCheck;
    private LoopCuttingCheck loopCheck;
    private boolean cut = false;

    public StripMineLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    public void setStripFactor(int factor) {
        numFactor = factor;
    }
    
    public void setNewName(String name) {
    	newName = name;
    }
    
    @Override
    public void setSecondOption(boolean cut) {
    	this.cut = cut;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
    	if (cut) {
    		loopCheck = new LoopCuttingCheck(getLoop());
    		loopCheck.performChecks(status, pm, new LoopCuttingParams(numFactor, newName));
    	}
    	else {
    		stripCheck = new StripMineCheck(getLoop());
            stripCheck.performChecks(status, pm, new StripMineParams(numFactor, newName));
    	}
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
    	if (cut) {
    		new LoopCuttingAlteration(rewriter, numFactor, newName, loopCheck).change();
    	}
    	else {
            new StripMineAlteration(rewriter, numFactor, newName, stripCheck).change();

    	}
    }
}
