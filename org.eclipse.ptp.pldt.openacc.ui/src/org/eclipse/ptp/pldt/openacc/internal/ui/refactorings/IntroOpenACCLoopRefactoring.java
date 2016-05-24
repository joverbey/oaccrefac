/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 ******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroOpenACCLoopAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroOpenACCLoopCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroduceKernelsLoopAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.IntroduceKernelsLoopCheck;

/**
 * Refactoring that adds a <code>#pragma acc parallel</code> directive to a for-loop.
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
public class IntroOpenACCLoopRefactoring extends ForLoopRefactoring {

    private IntroOpenACCLoopCheck check;
    private IntroduceKernelsLoopCheck kcheck;
    private boolean kernels = false;
    
    public IntroOpenACCLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }
    
    @Override
    public void setSecondOption(boolean kernels) {
    	this.kernels = kernels;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
    	if (kernels) {
    		kcheck = new IntroduceKernelsLoopCheck(getLoop());
    		kcheck.performChecks(status, pm, null);
    	} else {
	        check = new IntroOpenACCLoopCheck(getLoop());        
	        check.performChecks(status, pm, null);
    	}
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
    	if (kernels) {
    		new IntroduceKernelsLoopAlteration(rewriter, kcheck).change();
    	} else {
    		new IntroOpenACCLoopAlteration(rewriter, check).change();
    	}
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null; // Refactoring history is not supported.
    }
}
