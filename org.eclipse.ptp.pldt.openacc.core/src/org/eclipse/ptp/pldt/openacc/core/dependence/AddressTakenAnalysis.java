/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.dependence;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * AddressTakenAnalysis performs a simple address-taken analysis on a given function.
 * <p>
 * The progress of analyzing the function is stored in the monitor.
 * 
 * @author John William O'Rourke
 * @author Jeff Overbey
 */
public class AddressTakenAnalysis {

    private static Map<IASTFunctionDefinition, AddressTakenAnalysis> cache = new WeakHashMap<>();

    public static AddressTakenAnalysis forFunction(IASTFunctionDefinition function, IProgressMonitor monitor) {
        if (!cache.containsKey(function))
            cache.put(function, new AddressTakenAnalysis(function, monitor));
        return cache.get(function);
    }

    /**
     * addressTakenVariables holds all of the variables that are discovered to have had their addresses taken in the
     * function.
     */
    private final Set<IVariable> addressTakenVariables;

    /**
     * variables holds all variables found in the function.
     */
    private final Set<IVariable> variables;

    /**
     * AddressTakenAnalysis performs the analysis on the given function and monitor.
     * 
     * The results of the analysis are constant, so a new PointsToAnalysis object will have to be created for further
     * analysis.
     * 
     * @param function
     *            IASTFunctionDefinition to perform analysis on.
     * @param monitor
     *            IProgressMonitor for analysis project. May be null.
     */
    private AddressTakenAnalysis(IASTFunctionDefinition function, IProgressMonitor monitor) {
        if (function == null) {
            throw new IllegalArgumentException("function may not be null."); //$NON-NLS-1$
        }
        addressTakenVariables = new HashSet<>();
        variables = new HashSet<>();
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask(Messages.AddressTakenAnalysis_PointsToAnalysis, IProgressMonitor.UNKNOWN);
        try {
            performAnalysis(function, monitor);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } finally {
            monitor.done();
        }
    }

    /**
     * isAddressTaken reports the results of the analysis for a given IVariable.
     * 
     * Throws an IllegalArgumentException if the given variable is null.
     * 
     * @param variable
     *            IVariable to check if its address has been taken.
     * @return true if variable had its address taken anywhere in the function body.
     */
    public boolean isAddressTaken(IVariable variable) {
        if (variable == null) {
            throw new IllegalArgumentException("variable may not be null."); //$NON-NLS-1$
        }
        if (!variables.contains(variable)) {
            throw new IllegalArgumentException("variable, called " + variable.getName() + ", is not a local variable."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return addressTakenVariables.contains(variable);
    }

    /**
     * performAnalysis performs the address taken analysis recursively on the given function.
     * 
     * @param function
     *            IASTNode to check for variables with their addresses taken in.
     * @param monitor
     *            IProgressMonitor which holds the analysis progress.
     * @throws IllegalArgumentException
     */
    private void performAnalysis(IASTNode current, IProgressMonitor monitor) {
        monitor.worked(1);
        if (current instanceof IASTName) {
            IBinding binding = ((IASTName) current).resolveBinding();
            if (binding instanceof IVariable) {
                variables.add((IVariable) binding);
            }
        }
        if (current instanceof IASTUnaryExpression) {
            IASTUnaryExpression unary = (IASTUnaryExpression) current;
            if (unary.getOperator() == IASTUnaryExpression.op_amper) {
                if (unary.getOperand().isLValue()) {
                    findAddressBinding(unary.getOperand(), false);
                } else {
                    throw new IllegalArgumentException(
                            "Address of non LValue (" + unary.getRawSignature() + ") taken."); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
        for (IASTNode other : current.getChildren()) {
            performAnalysis(other, monitor);
        }
    }

    /**
     * findAddressBinding finds the binding which had its address taken in current and adds it to addressTakenVariables
     * 
     * @param current
     *            IASTExpression to search in.
     * @param foundBinary
     *            Whether a unary or binary expression is being searched in.
     */
    private void findAddressBinding(IASTExpression current, boolean foundBinary) {
        if (current instanceof IASTUnaryExpression) {
            IASTUnaryExpression unary = (IASTUnaryExpression) current;
            findAddressBinding(unary.getOperand(), foundBinary);
        } else if (current instanceof IASTBinaryExpression) {
            IASTBinaryExpression binary = (IASTBinaryExpression) current;
            if (binary.getOperand1().isLValue()) {
                findAddressBinding(binary.getOperand1(), true);
            }
            if (binary.getOperand2().isLValue()) {
                findAddressBinding(binary.getOperand2(), true);
            }
        } else {
            if (current instanceof IASTIdExpression) {
                IBinding binding = ((IASTIdExpression) current).getName().resolveBinding();
                if (binding instanceof IVariable) {
                    IVariable variable = (IVariable) binding;
                    if (foundBinary && variable.getType() instanceof IPointerType || !foundBinary) {
                        addressTakenVariables.add(variable);
                    }
                }
            }
        }
    }

    /**
     * Returns the set of all local variables whose address is taken.
     */
    public Set<IVariable> getAddressTakenVariables() {
        return addressTakenVariables;
    }
}
