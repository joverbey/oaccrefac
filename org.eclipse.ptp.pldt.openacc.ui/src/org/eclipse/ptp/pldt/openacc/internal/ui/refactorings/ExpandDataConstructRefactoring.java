/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.transformations.ExpandDataConstructAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.ExpandDataConstructCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;

@SuppressWarnings("restriction")
public class ExpandDataConstructRefactoring extends PragmaDirectiveRefactoring {


    private ExpandDataConstructCheck check;
    
    public ExpandDataConstructRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        
        if (selection == null || tu.getResource() == null || project == null)
            initStatus.addFatalError("Invalid selection");
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }
    
    @Override
    public void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new ExpandDataConstructCheck(getPragma(), getStatement());
        check.performChecks(initStatus, pm, null);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new ExpandDataConstructAlteration(rewriter, check).change();
    }


}
