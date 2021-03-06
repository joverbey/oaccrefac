/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
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
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DataDependence;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.Direction;

public class InterchangeLoopsCheck extends ForLoopCheck<InterchangeLoopParams> {

    private ForStatementInquisitor inq;
    private IASTForStatement outer;
    private IASTForStatement inner;

    public InterchangeLoopsCheck(RefactoringStatus status, IASTForStatement outer) {
        // passing the second loop in also causes duplicate variable accesses when doing the dependence analysis
        super(status, outer);
        this.outer = outer;
        this.inq = ForStatementInquisitor.getInquisitor(outer);
    }
    
    @Override
    protected void doParameterCheck(InterchangeLoopParams params) {
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.getLoop());
        List<IASTForStatement> headers = inq.getPerfectlyNestedLoops();
        if (params.getDepth() < 0 || params.getDepth() >= headers.size()) {
            status.addFatalError(Messages.InterchangeLoopsCheck_NoForLoopAtNestDepth + params.getDepth() + Messages.InterchangeLoopsCheck_Period);
            return;
        }
        
        this.inner = ASTUtil.findDepth(outer, IASTForStatement.class, params.getDepth());
    }
    
    @Override
    protected void doLoopFormCheck() {
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(outer);

        if (!(inq.getPerfectlyNestedLoops().contains(inner))) {
            status.addFatalError(Messages.InterchangeLoopsCheck_OnlyPerfectlyNested);
            return;
        }
 
        // OpenACC pragmas do not need to be checked for on either loop because
        //
        // Outer loop carries dependence:
        //
        // do i = 1, 10
        //     parallel do j = 1, 10
        //         a[i][j] = a[i-1][j]    ; Carried by i-loop
        //
        // If the outer loop carries the dependence, and the loops are 
        // interchanged, then the j-loop will still run in parallel, which
        // is fine because the i-loop carries the dependence and it is still
        // not parallel.
        //
        // Inner loop carries dependence:
        //
        // parallel do i = 1, 10
        //     do j = 1, 10
        //         a[i][j] = a[i][j-1]    ; Carried by j-loop
        //
        // If the inner loop carries the dependence, and the loops are 
        // interchanged, hen the i-loop will still run in parallel, which
        // is fine because the j-loop carries the dependence and it is still
        // not parallel.
        //
        // If both carry dependence:
        //
        // There are no pragmas anyways in this case, so they don't need to be
        // investigated.
        //
        // If both have pragmas:
        //
        // Interchange is fine in all cases because neither loop carries a 
        // dependence.
        
    }

    @Override
    protected void doDependenceCheck(DependenceAnalysis dep) {

        List<IASTForStatement> headers = inq.getPerfectlyNestedLoops();
        int second_depth = headers.indexOf(inner);
        if (second_depth > 0) {
            int numEnclosingLoops = countEnclosingLoops(outer);
            boolean isValid = isInterchangeValid(numEnclosingLoops, second_depth + numEnclosingLoops,
                    dep.getDependences());
            if (!isValid) {
                status.addError(Messages.InterchangeLoopsCheck_InterchangingSelectedLoopWith + Messages.InterchangeLoopsCheck_Depth + second_depth
                        + Messages.InterchangeLoopsCheck_WillChangeDependenceStructure + Messages.InterchangeLoopsCheck_OfLoopNest);
            }
        } else {
            throw new IllegalArgumentException(
                    Messages.InterchangeLoopsCheck_SecondForMustBeWithin + Messages.InterchangeLoopsCheck_PerfectlyNestedLoopHeadersOfFirst);
        }
    }

    private int countEnclosingLoops(IASTNode outsideOf) {
        int result = 0;
        for (IASTNode node = outsideOf.getParent(); node != null; node = node.getParent()) {
            if (node instanceof IASTForStatement) {
                result++;
            }
        }
        return result;
    }

    private boolean isInterchangeValid(int i, int j, Set<DataDependence> dependences) {
        for (DataDependence dep : dependences) {
            Direction[] dirVec = dep.getDirectionVector();
            if (isValid(dirVec) && !isValid(swap(i, j, dirVec))) {
                return false;
            }
        }
        return true;
    }

    private boolean isValid(Direction[] dirVec) {
        for (Direction dir : dirVec) {
            switch (dir) {
            case EQ:
                continue;
            case LT:
            case LE:
            case ANY:
                return true;
            case GT:
            case GE:
                return false;
            default:
                throw new IllegalStateException();
            }
        }
        return true;
    }

    private Direction[] swap(int i, int j, Direction[] dirVec) {
        if (i < 0 || j < 0 || i >= dirVec.length || j >= dirVec.length) {
            throw new IllegalArgumentException();
        }

        Direction[] result = new Direction[dirVec.length];
        System.arraycopy(dirVec, 0, result, 0, dirVec.length);
        Direction tmp = result[i];
        result[i] = result[j];
        result[j] = tmp;
        return result;
    }

    public IASTForStatement getOuterLoop() {
        return outer;
    }

    public IASTForStatement getInnerLoop() {
        return inner;
    }
    
}
