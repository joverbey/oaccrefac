/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

package edu.auburn.oaccrefac.core.dependence;

import java.util.HashSet;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * AddressTakenAnalysis performs an address taken analysis on a given function.
 * 
 * The progress of analyzing the function is stored in the monitor.
 */
public class AddressTakenAnalysis {
    
    /**
     * addressTakenVariables holds all of the variables that are discovered to
     * have had their addresses taken in the function.
     */
    private final HashSet<IVariable> addressTakenVariables;
    
    /**
     * variables holds all variables found in the function.
     */
    private final HashSet<IVariable> variables;
    
    /**
     * PointsToAnalysis performs the analysis on the given function and monitor.
     * 
     * The results of the analysis are constant, so a new PointsToAnalysis object will
     * have to be created for further analysis.
     * 
     * @param function IASTFunctionDefinition to perform analysis on.
     * @param monitor IProgressMonitor for analysis project. May be null.
     */
    public AddressTakenAnalysis(IASTFunctionDefinition function, IProgressMonitor monitor) {
        if (function == null) {
            throw new IllegalArgumentException("function may not be null.");
        }
        addressTakenVariables = new HashSet<>();
        variables = new HashSet<>();
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask("Points To Analysis", IProgressMonitor.UNKNOWN);
        performAnalysis(function, monitor);
        monitor.done();
    }
    
    /**
     * addressTaken reports the results of the analysis for a given IVariable.
     * 
     * Throws an IllegalArgumentException if the given variable is null.
     * 
     * @param variable IVariable to check if its address has been taken.
     * @return true if variable had its address taken anywhere in the function body.
     */
    public boolean addressTaken(IVariable variable) {
        if (variable == null) {
            throw new IllegalArgumentException("variable may not be null.");
        }
        if (!variables.contains(variable)) {
            throw new IllegalArgumentException(
                    "variable, called " + variable.getName() + ", is not a local variable."
            );
        }
        return addressTakenVariables.contains(variable);
    }
    
    /**
     * performAnalysis performs the address taken analysis recursively on the
     * given function.
     * 
     * @param function IASTFunctionDefinition to check for variables with their
     * addresses taken in.
     * @param monitor IProgressMonitor which holds the analysis progress.
     */
    private void performAnalysis(IASTFunctionDefinition function, IProgressMonitor monitor) {
        // start by printing all names and types below & symbol
    }
}
