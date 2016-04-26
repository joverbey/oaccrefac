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
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.InterchangeLoopParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.OpenACCToOpenMPDirectiveAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.OpenACCToOpenMPDirectiveCheck;


/**
 * Refactoring that change a <code>#pragma acc parallel loop</code> directive to <code>#pragma omp parallel for</code> to a for-loop.
 * 
 * @author Nawrin Sultana
 */

public class OpenACCToOpenMPLoopDirectiveRefactoring extends SourceFileRefactoring{
    
    private OpenACCToOpenMPDirectiveCheck check;
    private int depth = 1;
    
    public OpenACCToOpenMPLoopDirectiveRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }
    
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
            check = new OpenACCToOpenMPDirectiveCheck(getPragma());
            check.performChecks(status, pm, new InterchangeLoopParams(depth));   
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new OpenACCToOpenMPDirectiveAlteration(rewriter, check).change();
        
    }
    
    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null; // Refactoring history is not supported.
    }

}
