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
package edu.auburn.oaccrefac.core.dependence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;
import edu.auburn.oaccrefac.internal.core.dependence.DirectionHierarchyTester;
import edu.auburn.oaccrefac.internal.core.dependence.VariableAccess;

/**
 * Analyzes data dependences between statements.
 * 
 * @author Alexander Calvert
 * @author Jeff Overbey
 */
public class DependenceAnalysis extends AbstractDependenceAnalysis {

    /**
     * Constructor. Analyzes dependences in a sequence of C statements.
     * 
     * @throws DependenceTestFailure
     */
    public DependenceAnalysis(IProgressMonitor pm, IASTStatement... statements) throws DependenceTestFailure, OperationCanceledException {
        super(pm, statements);
        pm.subTask("Analyzing dependences...");
        computeDependences(pm);
    }

    @Override
    protected void computeDependences(IProgressMonitor pm) throws DependenceTestFailure {
        SubMonitor progress = SubMonitor.convert(pm, getVariableAccesses().size() * getVariableAccesses().size());

        for (VariableAccess v1 : getVariableAccesses()) {
            progress.subTask(String.format("Analyzing line %d - %s",
                    v1.getVariableName().getFileLocation().getStartingLineNumber(), v1));
            for (VariableAccess v2 : getVariableAccesses()) {
                if (v1.refersToSameVariableAs(v2) && (v1.isWrite() || v2.isWrite()) && feasibleControlFlow(v1, v2)) {
                    DependenceType dependenceType = v1.getDependenceTypeTo(v2);
                    if (v1.isScalarAccess() || v2.isScalarAccess()) {
                        Direction[] directionVector = new Direction[v1.numEnclosingLoops()];
                        Arrays.fill(directionVector, Direction.ANY);
                        addDependence(new DataDependence(v1, v2, directionVector, dependenceType));
                    } else {
                        List<IASTForStatement> commonLoops = v1.getCommonEnclosingLoops(v2);
                        List<IBinding> indexVars = ASTUtil.getLoopIndexVariables(commonLoops);
                        Set<IBinding> otherVars = collectAllVariables(v1.getLinearSubscriptExpressions(),
                                v2.getLinearSubscriptExpressions());
                        otherVars.removeAll(indexVars);

                        List<IBinding> vars = new ArrayList<IBinding>(indexVars.size() + otherVars.size());
                        vars.addAll(indexVars);
                        vars.addAll(otherVars);

                        int[][] writeCoefficients = v1.collectCoefficients(vars);
                        int[][] readCoefficients = v2.collectCoefficients(vars);

                        int[] lowerBounds = fillArray(commonLoops.size(), Integer.MIN_VALUE + 1);
                        int[] upperBounds = fillArray(commonLoops.size(), Integer.MAX_VALUE - 1);
                        for (int i = 0; i < commonLoops.size(); i++) {
                            ForStatementInquisitor thisLoop = InquisitorFactory.getInquisitor(commonLoops.get(i));
                            Long lb = thisLoop.getLowerBound();
                            Long ub = thisLoop.getInclusiveUpperBound();
                            if (lb != null && Integer.MIN_VALUE + 1 <= lb.longValue()
                                    && lb.longValue() <= Integer.MAX_VALUE - 1) {
                                lowerBounds[i] = (int) lb.longValue();
                            }
                            if (ub != null && Integer.MIN_VALUE + 1 <= ub.longValue()
                                    && ub.longValue() <= Integer.MAX_VALUE - 1) {
                                upperBounds[i] = (int) ub.longValue();
                            }
                        }

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
    }
    
    /**
     * Returns whether the a statement reaches another one. Returns false if the statements 
     * are not a read and a write, respectively.  
     * @param write
     * @param read
     * @return
     */
    public boolean reaches(VariableAccess write, VariableAccess read) {
        for(DataDependence dep : getDependences()) {
            if(dep.getAccess1().equals(write) && dep.getAccess2().equals(read) && dep.getType() == DependenceType.FLOW) {
                return true;
            }
        }
        return false;
    }
    
//    /**
//     * Gets all the statements reached by this definition
//     * @param write
//     * @return
//     */
//    public Set<IASTStatement> getReachedStatements(IASTStatement write) {
//        Set<IASTStatement> reached = new HashSet<IASTStatement>();
//        for(DataDependence dep : getDependences()) {
//            if(dep.getStatement1().equals(write) && dep.getType() == DependenceType.FLOW) {
//                reached.add(dep.getStatement2());
//            }
//        }
//        return reached;
//    }
//    
//    /**
//     * Gets all the definitions that reach the given statement
//     * @param read
//     * @return
//     */
//    public Set<IASTStatement> getReachingDefinitions(IASTStatement read) {
//        Set<IASTStatement> reachingDefs = new HashSet<IASTStatement>();
//        for(DataDependence dep : getDependences()) {
//            if(dep.getStatement2().equals(read) && dep.getType() == DependenceType.FLOW) {
//                reachingDefs.add(dep.getStatement1());
//            }
//        }
//        return reachingDefs;
//    }
}
