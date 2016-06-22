package org.eclipse.ptp.pldt.openacc.internal.core;

/**
 * Thrown when dependence testing cannot succeed.
 */
@SuppressWarnings("serial")
public class FunctionGraphException extends Exception
{
    public FunctionGraphException(String message)
    {
        super(message);
    }
}
