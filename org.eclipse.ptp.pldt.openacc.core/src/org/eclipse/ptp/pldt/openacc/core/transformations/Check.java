/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class Check<T extends RefactoringParams> {

	protected RefactoringStatus status;
	
	public Check(RefactoringStatus status) {
		this.status = status;
	}
	
    protected void doParameterCheck(T params) { }
    
    public abstract IASTTranslationUnit getTranslationUnit();
    
    public RefactoringStatus parameterCheck(IProgressMonitor pm, T params) {
        doParameterCheck(params);
        return status;
    }
    
    public RefactoringStatus performChecks(IProgressMonitor pm, T params) {
        parameterCheck(pm, params);
        return status;
    }
    
    public RefactoringStatus getStatus() {
    	return status;
    }

}
