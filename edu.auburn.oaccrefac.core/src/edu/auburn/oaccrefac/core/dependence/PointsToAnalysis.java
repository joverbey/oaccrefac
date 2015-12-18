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

import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Performs points-to analysis.
 */
public class PointsToAnalysis {

    /**
     * Stores points-to analysis results.
     */
    private HashMap<IVariable, Boolean> analysisResults;
    
    /**
     * Function analyzed.
     */
    private IFunction function;
    
    /**
     * Progress monitor for analysis.
     */
    private IProgressMonitor monitor;
    
    /**
     * Performs a points-to analysis on the given function. Use mayBeAliased
     * to get results of analysis for a specific variable.
     */
    public PointsToAnalysis(IFunction function, IProgressMonitor monitor) {
        analysisResults = new HashMap<>();
        this.function = function;
        this.monitor = monitor;
        performAnalysis();
    }
    
    /**
     * Retrieves analysis results for a sepcific variable.
     * 
     * @param variable
     * @return
     */
    public boolean mayBeAliased(IVariable variable) {
        return false;
    }
    
    /**
     * Helper method for performing a points-to analysis.
     */
    private void performAnalysis() {
        
    }
    
}
