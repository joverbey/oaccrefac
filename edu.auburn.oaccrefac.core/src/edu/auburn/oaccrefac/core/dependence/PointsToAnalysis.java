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
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Performs points-to analysis on the IASTFunctionDefinition passed to the constructor.
 */
public class PointsToAnalysis {

    /**
     * Stores IVariables which may be aliased.
     * 
     * This is only modified when the constructor is called.
     */
    private HashSet<IVariable> mayBeAliased;
    
    /**
     * Stores all local variables found in function.
     * 
     * This is only modified when the constructor is called.
     */
    private HashSet<IVariable> variables;
       
    /**
     * Performs a points-to analysis on the given function. 
     * 
     * Use mayBeAliased to get results of analysis for a specific variable.
     * 
     * The results of the analysis are constant, so a new PointsToAnalysis object will
     * have to be created for further analysis.
     * 
     * @param function IASTFunctionDefinition to perform analysis on.
     * @param monitor IProgressMonitor for analysis project. May be null.
     */
    public PointsToAnalysis(IASTFunctionDefinition function, IProgressMonitor monitor) {
        mayBeAliased = new HashSet<>();
        variables = new HashSet<>();
        if (monitor != null) {
            monitor.beginTask("Points To Analysis", IProgressMonitor.UNKNOWN);
        }
        performAnalysis(monitor, function, false);
        if (monitor != null) {
            monitor.done();
        }
    }
    
    /**
     * Retrieves analysis results for a specific local IVariable.
     * 
     * @param variable IVariable to find results of analysis for.
     * @return Whether or not the given IVariable may be aliased.
     */
    public boolean mayBeAliased(IVariable variable) {
        if (!variables.contains(variable)) {
            throw new IllegalArgumentException(variable.getName() + " is not a local variable.");
        }
        return mayBeAliased.contains(variable);
    }
    
    /**
     * Helper method for performing a points-to analysis.
     * 
     * True if true in default map or not in default map
     * False otherwise
     * 
     * Algorithm:
     *  Traverse function given
     *      look for &
     *      if &, continue recursively descending
     *      find which variable has address taken
     *      add to set
     *  Also track what is and isn't local variable so exception can be thrown.
     * 
     * @param monitor IProgressMonitor which monitors progress of analysis.
     * @param current Current IASTNode being investigated.
     * @param foundOperator Found state of address of operator.
     */
    private void performAnalysis(IProgressMonitor monitor, IASTNode current, boolean foundOperator) {
        monitor.worked(1);
    }
    
}
