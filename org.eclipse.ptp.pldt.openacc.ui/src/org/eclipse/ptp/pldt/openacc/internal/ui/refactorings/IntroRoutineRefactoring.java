/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 *     Carl Worley (Auburn) - initial API and implementation
 ******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IntroRoutineAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IntroRoutineCheck;

/**
 * Refactoring that adds a <code>#pragma acc parallel</code> directive to a for-loop.
 * 
 * @author Carl Worley
 */
public class IntroRoutineRefactoring extends StatementsRefactoring {

	private IntroRoutineCheck check;

	public IntroRoutineRefactoring(ICElement element, ISelection selection, ICProject project) {
		super(element, selection, project);
	}
	
	@Override
    public void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new IntroRoutineCheck(status, getStatements(), getAllEnclosedNodes());
        check.performChecks(pm, null);
    }

	@Override
	protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
		new IntroRoutineAlteration(rewriter, check).change();
	}
	
}