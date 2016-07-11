/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.internal.core;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

/**
 * Thrown when dependence testing cannot succeed.
 */
@SuppressWarnings("serial")
public class FunctionGraphException extends Exception
{
    private RefactoringStatusContext context = null;

	public FunctionGraphException(String message)
    {
        super(message);
    }
    
    public FunctionGraphException(String message, RefactoringStatusContext context)
    {
        super(message);
        
        this.context = context;
    }
    
    public RefactoringStatusContext getContext() {
    	return context;
    }
}
