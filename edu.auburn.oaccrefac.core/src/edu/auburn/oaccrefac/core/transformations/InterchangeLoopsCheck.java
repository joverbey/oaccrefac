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
 *******************************************************************************/
package edu.auburn.oaccrefac.core.transformations;

import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DataDependence;
import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.Direction;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class InterchangeLoopsCheck extends ForLoopCheck<InterchangeLoopParams> {

    private ForStatementInquisitor inq;
    private IASTForStatement outer;
    private IASTForStatement inner;

    public InterchangeLoopsCheck(IASTForStatement outer) {
        // passing the second loop in also causes duplicate variable accesses when doing the dependence analysis
        super(outer);
        this.outer = outer;
        this.inq = InquisitorFactory.getInquisitor(outer);
    }
    
    @Override
    protected void doParameterCheck(RefactoringStatus status, InterchangeLoopParams params) {
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.getLoop());
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (params.getDepth() < 0 || params.getDepth() >= headers.size()) {
            status.addFatalError("There is no for-loop at exchange depth:" + params.getDepth());
            return;
        }
        
        this.inner = ASTUtil.findDepth(outer, IASTForStatement.class, params.getDepth());
        
    }
    
    @Override
    protected void doLoopFormCheck(RefactoringStatus status) {
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(outer);
        if (!inq.isPerfectLoopNest()) {
            status.addFatalError("Only perfectly nested loops can be interchanged.");
            return;
        }
    }

    @Override
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {

        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        int second_depth = headers.indexOf(inner);
        if (second_depth > 0) {
            int numEnclosingLoops = countEnclosingLoops(outer);
            boolean isValid = isInterchangeValid(numEnclosingLoops, second_depth + numEnclosingLoops,
                    dep.getDependences());
            if (!isValid) {
                status.addError("Interchanging the selected loop with the loop at " + "depth " + second_depth
                        + " will change the dependence structure " + "of the loop nest.");
            }
        } else {
            throw new IllegalArgumentException(
                    "Second for-statement must be within" + "perfectly nested loop headers of first.");
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
