/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 *     John William O'Rourke (Auburn) - Initial API and implementation
 ******************************************************************************/
package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.IntroduceKernelsLoopAlteration;
import edu.auburn.oaccrefac.core.transformations.IntroduceKernelsLoopCheck;

/**
 * Refactoring that adds a <code>#pragma acc kernels loop</code> directive to a for-loop.
 * 
 * @author jwowillo
 */
public class IntroduceKernelsLoopRefactoring extends ForLoopRefactoring {
    
    private IntroduceKernelsLoopCheck check;
    
    public IntroduceKernelsLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new IntroduceKernelsLoopCheck(getLoop());
        check.performChecks(status, pm, null);
    }
    
    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        IntroduceKernelsLoopAlteration change = new IntroduceKernelsLoopAlteration(rewriter, check);
        change.change();
    }
    
    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null; // Refactoring history is not supported.
    }
    
}
