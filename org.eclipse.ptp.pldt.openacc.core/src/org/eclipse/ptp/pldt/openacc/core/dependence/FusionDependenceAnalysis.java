/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
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
package org.eclipse.ptp.pldt.openacc.core.dependence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.InquisitorFactory;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DirectionHierarchyTester;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.VariableAccess;

/**
 * Analyzes data dependences between statements.
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 * @author Alexander Calvert
 */
public class FusionDependenceAnalysis extends AbstractDependenceAnalysis {

    private static final int NEST_LVL = 1;
    private IBinding indexVar;
    private Long lowerBound;
    private Long inclusiveUpperBound;
    

    /**
     * Analyzes dependences in a sequence of C statements.
     * 
     * @throws DependenceTestFailure
     */
    public FusionDependenceAnalysis(IProgressMonitor pm, IASTForStatement loopStatement, IASTStatement... statements)
            throws DependenceTestFailure, OperationCanceledException {
        super(pm, statements);
        
        ForStatementInquisitor loop = InquisitorFactory.getInquisitor(loopStatement);

        this.indexVar = loop.getIndexVariable();
        this.lowerBound = loop.getLowerBound();
        this.inclusiveUpperBound = loop.getInclusiveUpperBound();

        pm.subTask("Analyzing dependences...");
        computeDependences(pm);
    }
    
    /**
     * 
     * Does the same analysis as <code>DependenceAnalysis.computeDependences</code>, but 
     * does not use anything regarding the common enclosing loops
     * Also assumes there is no loop nesting and that there is no outer-scope variable that
     * shares the same name as the index variable.
     */
    @Override
    protected void computeDependences(IProgressMonitor pm) throws DependenceTestFailure {
        SubMonitor progress = SubMonitor.convert(pm, getVariableAccesses().size() * getVariableAccesses().size());
        for (VariableAccess v1 : getVariableAccesses()) {
            progress.subTask(String.format("Analyzing line %d - %s",
                    v1.getVariableName().getFileLocation().getStartingLineNumber(), v1));
            for (VariableAccess v2 : getVariableAccesses()) {
                if (v1.refersToSameVariableAs(v2) && (v1.isWrite() || v2.isWrite())) {
                    DependenceType dependenceType = v1.getDependenceTypeTo(v2);
                    if (v1.isScalarAccess() || v2.isScalarAccess()) {
                        Direction[] directionVector = new Direction[v1.numEnclosingLoops()];
                        Arrays.fill(directionVector, Direction.ANY);
                        addDependence(new DataDependence(v1, v2, directionVector, dependenceType));
                    } else {
                        
                        Set<IBinding> otherVars = collectAllVariables(v1.getLinearSubscriptExpressions(),
                                v2.getLinearSubscriptExpressions());
                        otherVars.remove(indexVar);

                        List<IBinding> vars = new ArrayList<IBinding>(otherVars.size() + NEST_LVL);
                        vars.add(indexVar);
                        vars.addAll(otherVars);

                        int[][] writeCoefficients = this.collectCoefficients(v1, vars);
                        int[][] readCoefficients = this.collectCoefficients(v2, vars);
                        
                        
                        int[] lowerBounds = fillArray(NEST_LVL, Integer.MIN_VALUE + 1);
                        int[] upperBounds = fillArray(NEST_LVL, Integer.MAX_VALUE - 1);
                        for (int i = 0; i < NEST_LVL; i++) {
                            if (lowerBound != null && Integer.MIN_VALUE + 1 <= lowerBound.longValue()
                                    && lowerBound.longValue() <= Integer.MAX_VALUE - 1) {
                                lowerBounds[i] = (int) lowerBound.longValue();
                            }
                            if (inclusiveUpperBound != null && Integer.MIN_VALUE + 1 <= inclusiveUpperBound.longValue()
                                    && inclusiveUpperBound.longValue() <= Integer.MAX_VALUE - 1) {
                                upperBounds[i] = (int) inclusiveUpperBound.longValue();
                            }
                        }

                        Set<IBinding> otherVarsTmp = new HashSet<IBinding>();
                        for(IBinding name : otherVars) {
                            if(!name.getName().equals(indexVar.getName())) {
                                otherVarsTmp.add(name);
                            }
                        }
                        otherVars = otherVarsTmp;
                        
                        DirectionHierarchyTester dht = new DirectionHierarchyTester(lowerBounds, upperBounds,
                                writeCoefficients, readCoefficients, otherVars.size());
                        Set<Direction[]> dvs = dht.getPossibleDependenceDirections();
                        for (Direction[] directionVector : dvs) {
                            addDependence(new DataDependence(v1, v2, directionVector, dependenceType));
                        }
                    }
                }

                progress.worked(1);
                if (progress.isCanceled()) {
                    throw new OperationCanceledException("Dependence test cancelled.");
                }
            }
        }
        return;
    }
    
    private int[][] collectCoefficients(VariableAccess v, List<IBinding> vars) {
        int[][] result = new int[v.getLinearSubscriptExpressions().length][];
        for (int i = 0; i < v.getLinearSubscriptExpressions().length; i++) {
            result[i] = collectCoefficients(v, vars, i);
        }
        return result;
    }

    private int[] collectCoefficients(VariableAccess v, List<IBinding> vars, int subscript) {
        List<Integer> result = new ArrayList<Integer>();
        Map<IBinding, Integer> coeffs = v.getLinearSubscriptExpressions()[subscript].getCoefficients();
        result.add(v.getLinearSubscriptExpressions()[subscript].getConstantCoefficient());
        result.add(getIndexVariableCoefficient(vars, coeffs));
        for(IBinding var : vars) {
            Integer coeff = coeffs.get(var);
            if(var.getName().equals(indexVar.getName())) {
                continue;
            }
            else {
                if(coeff == null) {
                    result.add(0);
                }
                else {
                    result.add(coeff);
                }
            }
        }
        
        int[] actualResult = new int[result.size()];
        for(int i = 0; i < actualResult.length; i++) {
            actualResult[i] = result.get(i);
        }
        return actualResult;
    }
    
    private int getIndexVariableCoefficient(List<IBinding> vars, Map<IBinding, Integer> coeffs) {
        List<IBinding> candidates = new ArrayList<IBinding>();
        
        //get all variables with the same name as the index var; should be two - an index variable from each loop
        for(IBinding var : vars) {
            if(var.getName().equals(indexVar.getName())) {
                candidates.add(var);
            }
        }
        
        //from those index variables, if they are all zero, the right coefficient is of course zero.
        //otherwise, only one of them will not be zero - the index var from the loop the coeffs
        //map's expression comes from. All others are not present in the expression.
        //So, the coefficient we want is the sole non-zero coefficient with the index var's name. 
        for(IBinding candidate : candidates) {
            if(coeffs.get(candidate) != null && coeffs.get(candidate) != 0) {
                return coeffs.get(candidate);
            }
        }
        return 0;
    }

}
