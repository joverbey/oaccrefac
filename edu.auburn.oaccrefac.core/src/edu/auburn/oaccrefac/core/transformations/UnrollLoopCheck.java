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

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class UnrollLoopCheck extends ForLoopCheck<UnrollLoopParams> {

    private final Long upperBound;

    public UnrollLoopCheck(IASTForStatement loop) {
        super(loop);
        IASTFunctionDefinition enclosing = ASTUtil.findNearestAncestor(loop, IASTFunctionDefinition.class);
        ConstantPropagation constantProp = new ConstantPropagation(enclosing);
        IASTExpression ubExpr = ((IASTBinaryExpression) loop.getConditionExpression()).getOperand2();
        upperBound = constantProp.evaluate(ubExpr);
    }

    @Override
    protected void doLoopFormCheck(RefactoringStatus status) {
        IASTStatement body = loop.getBody();
        // If the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement) {
            status.addFatalError("Loop body is empty -- nothing to unroll!");
            return;
        }

        // If the loop is not a counted loop, fail
        System.out.println();
        if (!InquisitorFactory.getInquisitor(loop).isCountedLoop()) {
            status.addFatalError("Loop form not supported");
            return;
        }

        // If the loop contains unsupported statements, fail
        IASTNode unsupported = InquisitorFactory.getInquisitor(loop).getFirstUnsupportedStmt();
        if (unsupported != null) {
            status.addFatalError("Loop contains unsupported statement: " + ASTUtil.toString(unsupported).trim());
            return;
        }

        // If the upper bound is not a constant, we cannot do loop unrolling
        if (upperBound == null) {
            status.addFatalError("Upper bound is not a constant value. Cannot perform unrolling!");
            return;
        }
        
        // If lower bound is not constant, we can't calculate the number of times to repeat the "trailer"
        //  after the unrolled loop
        if (InquisitorFactory.getInquisitor(loop).getLowerBound() == null) {
            status.addFatalError("Upper bound is not a constant value. Cannot perform unrolling!");
            return;
        }

    }

    @Override
    protected void doParameterCheck(RefactoringStatus status, UnrollLoopParams params) {
        // Check unroll factor validity...
        if (params.getUnrollFactor() <= 0) {
            status.addFatalError("Invalid loop unroll factor! (<= 0)");
            return;
        }
        
        // If we are unrolling more than the number of times the loop will 
        // run (upper bound - lower bound), we can't do the refactoring.
        long loopRunTimes = upperBound.longValue() - InquisitorFactory.getInquisitor(loop).getLowerBound().longValue();
        if (params.getUnrollFactor() > loopRunTimes) {
            status.addFatalError("Can't unroll loop more times than the loop runs!");
            return;
        }
    }

    
    public Long getUpperBound() {
        return upperBound;
    }
}
