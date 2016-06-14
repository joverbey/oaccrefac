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
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ConstantPropagation;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTPatternUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

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

		ForStatementInquisitor loopInquisitor = ForStatementInquisitor.getInquisitor(loop);
		
		// If the loop is not a counted loop, fail
		if (!loopInquisitor.isCountedLoop()) {
			status.addFatalError("Loop form not supported");
			return;
		}

		// If the loop contains unsupported statements, fail
		IASTNode unsupported = loopInquisitor.getFirstUnsupportedStmt();
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
		// after the unrolled loop
		if (loopInquisitor.getLowerBound() == null) {
			status.addFatalError("Upper bound is not a constant value. Cannot perform unrolling!");
			return;
		}
		
		IBinding indexBinding = loopInquisitor.getIndexVariable();
		class DefinitionFinder extends ASTVisitor {
			public boolean isDefinition;
			public DefinitionFinder() {
				this.shouldVisitNames = true;
			}
            @Override
            public int visit(IASTName name) {
            	if (ASTPatternUtil.isDefinition(name)) {
            		if (name.getBinding() != null && name.getBinding().equals(indexBinding)) {
                		this.isDefinition = true;
            		}
            	}
                return PROCESS_CONTINUE;
            }
        }
		DefinitionFinder finder = new DefinitionFinder();
		loop.getBody().accept(finder);
		if (finder.isDefinition) {
			status.addFatalError("Loop index variable is changed in the loop body. Cannot perform unrolling!");
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

		if (upperBound == null) {
			status.addFatalError("Can't determine loop upper bound.");
			return;
		}

		// If we are unrolling more than the number of times the loop will
		// run (upper bound - lower bound), we can't do the refactoring.
		long loopRunTimes = upperBound.longValue() - ForStatementInquisitor.getInquisitor(loop).getLowerBound().longValue();
		if (params.getUnrollFactor() > loopRunTimes) {
			status.addFatalError("Can't unroll loop more times than the loop runs");
			return;
		}
	}

	public Long getUpperBound() {
		return upperBound;
	}
}
