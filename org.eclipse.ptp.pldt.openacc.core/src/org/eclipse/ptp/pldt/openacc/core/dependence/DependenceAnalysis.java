/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.dependence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DirectionHierarchyTester;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.VariableAccess;

/**
 * Analyzes data dependences between statements.

 * @author Jeff Overbey
 * @author Adam Eichelkraut
 * @author Alexander Calvert
 */
public class DependenceAnalysis extends AbstractDependenceAnalysis {

	private IBinding index;
	
    /**
     * Analyzes dependences in a sequence of C statements.
     * 
     * @throws DependenceTestFailure
     */	
    public DependenceAnalysis(IProgressMonitor pm, IASTStatement... statements) throws DependenceTestFailure, OperationCanceledException {
        super(pm, statements);
        IASTForStatement loop = null;
        if (statements.length != 0) {
        	loop = ASTUtil.findNearestAncestor(statements[0], IASTForStatement.class);
        }
        index = ForStatementInquisitor.getInquisitor(loop).getIndexVariable();
        
        pm.subTask("Analyzing dependences...");
        computeDependences(pm);
    }

    @Override
    protected void computeDependences(IProgressMonitor pm) throws DependenceTestFailure {
        SubMonitor progress = SubMonitor.convert(pm, getVariableAccesses().size() * getVariableAccesses().size());

        for (VariableAccess v1 : getVariableAccesses()) {
            progress.subTask(String.format("Analyzing line %d - %s",
                    v1.getVariableName().getFileLocation().getStartingLineNumber(), v1));
            if (writesToIndex(v1)) {
            	throw new DependenceTestFailure(String.format("Loop cannot be analyzed.  Loop index variable is changed on line %d.",
                    v1.getVariableName().getFileLocation().getStartingLineNumber()));
            }
            for (VariableAccess v2 : getVariableAccesses()) {
                if (v1.refersToSameVariableAs(v2) && (v1.isWrite() || v2.isWrite()) && feasibleControlFlow(v1, v2)
                        //if the sink is a declaration of the same variable, there is no dependence
                        && !(v2.getEnclosingStatement() instanceof IASTDeclarationStatement)) {
                    DependenceType dependenceType = v1.getDependenceTypeTo(v2);
                    if (v1.isScalarAccess() || v2.isScalarAccess()) {
                        Direction[] directionVector = new Direction[v1.numEnclosingLoops()];
                        Arrays.fill(directionVector, Direction.ANY);
                        addDependence(new DataDependence(v1, v2, directionVector, dependenceType));
                    } else {
                        List<IASTForStatement> commonLoops = v1.getCommonEnclosingLoops(v2);
                        List<IBinding> indexVars = DependenceAnalysis.getLoopIndexVariables(commonLoops);
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
                            ForStatementInquisitor thisLoop = ForStatementInquisitor.getInquisitor(commonLoops.get(i));
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
     * Checks if a statement reaches another.
     * 
     * @param write Write statement.
     * @param read Read statement.
     * @return false if the statements are not a read and a write, respectively.
     */
    public boolean reaches(VariableAccess write, VariableAccess read) {
        for(DataDependence dep : getDependences()) {
            if(dep.getAccess1().equals(write) && dep.getAccess2().equals(read) && dep.getType() == DependenceType.FLOW) {
                return true;
            }
        }
        return false;
    }
    
    private boolean writesToIndex(VariableAccess write) {
    	return write.isWrite() && write.bindsTo(index);
    }

	private static List<IBinding> getLoopIndexVariables(List<IASTForStatement> loops) {
	    List<IBinding> result = new ArrayList<IBinding>(loops.size());
	    for (IASTForStatement forStmt : loops) {
	        ForStatementInquisitor loop = ForStatementInquisitor.getInquisitor(forStmt);
	        IBinding variable = loop.getIndexVariable();
	        if (variable != null) {
	            result.add(variable);
	        }
	    }
	    return result;
	}
}
