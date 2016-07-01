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
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.StripMineParams;

public class StripMineLoopRefactoring extends ForLoopRefactoring {

	private int stripFactor = -1;
	private boolean zeroBased = false;
	private boolean handleOverflow = true;
    private String newNameOuter = ""; //$NON-NLS-1$
    private String newNameInner = ""; //$NON-NLS-1$
    private StripMineCheck stripCheck;

    public StripMineLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    public void setStripFactor(int stripFactor) {
		this.stripFactor = stripFactor;
	}

	public void setZeroBased(boolean zeroBased) {
		this.zeroBased = zeroBased;
	}

	public void setHandleOverflow(boolean handleOverflow) {
		this.handleOverflow = handleOverflow;
	}

	public void setNewNameOuter(String newNameOuter) {
		this.newNameOuter = newNameOuter;
	}

	public void setNewNameInner(String newNameInner) {
		this.newNameInner = newNameInner;
	}

	@Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
		stripCheck = new StripMineCheck(status, getLoop());
        stripCheck.performChecks(pm, new StripMineParams(stripFactor, zeroBased, handleOverflow, newNameOuter, newNameInner));
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new StripMineAlteration(rewriter, stripFactor, zeroBased, handleOverflow, newNameOuter, newNameInner, stripCheck).change();
    }
}
