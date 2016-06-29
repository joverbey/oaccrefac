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
