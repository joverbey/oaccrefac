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
import org.eclipse.ptp.pldt.openacc.core.transformations.UnrollLoopAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.UnrollLoopCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.UnrollLoopParams;

/**
 * This class defines the implementation for refactoring a loop so that it is unrolled. For example:
 * 
 * <pre>
 * ORIGINAL:				REFACTORED:
 * int x;					|  int x;
 * for (x=0; x<100; x++)	|  for (x=0; x<100; x++) {
 *   delete(a[x]);			|    delete(a[x]); x++;
 *   						|    delete(a[x]); x++;
 *   						|	 delete(a[x]); x++;
 *   						|	 delete(a[x]); x++;
 *  						|	 delete(a[x]);
 *  						|  }
 * </pre>
 * 
 * (Example taken from Wikipedia's webpage on loop unrolling)
 */
public class UnrollLoopRefactoring extends ForLoopRefactoring {

    private int unrollFactor;
    private UnrollLoopCheck check;

    public UnrollLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    public void setUnrollFactor(int toSet) {
        unrollFactor = toSet;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new UnrollLoopCheck(getLoop());
        check.performChecks(status, pm, new UnrollLoopParams(unrollFactor));
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new UnrollLoopAlteration(rewriter, unrollFactor, check).change();
    }
}
