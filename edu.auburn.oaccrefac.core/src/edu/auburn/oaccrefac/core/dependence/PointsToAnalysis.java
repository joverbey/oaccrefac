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
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.c.ICASTPointer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Performs a very crude address-taken analysis on the IASTFunctionDefinition
 * passed to the constructor to determine if two variables, a and b, could
 * possibly point to the same thing.
 * 
 * The analysis works by finding any pointers that aren't restrict and adding
 * them to a set. If two queried IVariables are in this set, then the analysis
 * assumes they may point to the same variable.
 * 
 * The analysis can be made more thorough later.
 */
public class PointsToAnalysis {

    /**
     * notRestrictPointers holds all pointer variables without the restrict
     * qualifier in the IASTFunctionDefinition.
     */
    private final HashSet<IVariable> notRestrictPointers;
    
    /**
     * variables holds all variables found in the IASTFunctionDefinition.
     */
    private final HashSet<IVariable> variables;
       
    /**
     * PointsToAnalysis constructor.
     * 
     * The results of the analysis are constant, so a new PointsToAnalysis object will
     * have to be created for further analysis.
     * 
     * @param function IASTFunctionDefinition to perform analysis on.
     * @param monitor IProgressMonitor for analysis project. May be null.
     */
    public PointsToAnalysis(IASTFunctionDefinition function, IProgressMonitor monitor) {
        notRestrictPointers = new HashSet<>();
        variables = new HashSet<>();
        if (function == null) {
            throw new IllegalArgumentException("function may not be null.");
        }
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask("Points To Analysis", IProgressMonitor.UNKNOWN);
        performAnalysis(monitor, function);
        monitor.done();
    }
    
    /**
     * Retrieves analysis results for a specific local IVariable.
     * 
     * @param variable IVariable to find results of analysis for.
     * @return Whether or not the given IVariable may be aliased.
     */
    public boolean variablesMayPointToSame(IVariable a, IVariable b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Variables can't be null.");
        }
        if (!variables.contains(a)) {
            throw new IllegalArgumentException(
                    "a, called " + a.getName() + ", is not a local variable."
            );
        }
        if (!variables.contains(b)) {
            throw new IllegalArgumentException(
                    "b, called " + b.getName() + ", is not a local variable."
            );
        }
        return notRestrictPointers.contains(a) && notRestrictPointers.contains(b);
    }
    
    /**
     * performAnalysis does the crude analysis recursively.
     * 
     * @param monitor IProgressMonitor to check process of analysis.
     * @param current IASTNode being looked at.
     */
    private void performAnalysis(IProgressMonitor monitor, IASTNode current) {
        monitor.worked(1);
        boolean dontRecurse = false;
        if (current instanceof IASTName) {
            IBinding binding = ((IASTName) current).resolveBinding();
            if (binding instanceof IVariable) {
                dontRecurse = true;
                variables.add((IVariable) binding);
                if (isPointerAndNotRestrict(current.getParent())) {
                    notRestrictPointers.add((IVariable) binding);
                }
            }
        }
        if (!dontRecurse) {
            for (IASTNode child : current.getChildren()) {
                performAnalysis(monitor, child);
            }
        }
    }
    
    /**
     * isPointerAndNotRestrict checks if an IASTNode is a pointer without the
     * restrict qualifier.
     * 
     * @param current IASTNode being looked at.
     * 
     * @return True if the IASTNode is a pointer without restrict. False
     * otherwise.
     */
    private boolean isPointerAndNotRestrict(IASTNode current) {
        if (current instanceof ICASTPointer && !((ICASTPointer) current).isRestrict()) {
            return true;
        }
        for (IASTNode child : current.getChildren()) {
            boolean found = isPointerAndNotRestrict(child);
            if (found) {
                return found;
            }
        }
        return false;
    }
    
}
