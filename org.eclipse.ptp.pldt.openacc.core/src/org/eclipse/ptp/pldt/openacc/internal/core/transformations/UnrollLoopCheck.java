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
import org.eclipse.ptp.pldt.openacc.internal.core.ASTPatternUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.dataflow.ConstantPropagation;

public class UnrollLoopCheck extends ForLoopCheck<UnrollLoopParams> {

	private final Long upperBound;

	public UnrollLoopCheck(RefactoringStatus status, IASTForStatement loop) {
		super(status, loop);
		IASTFunctionDefinition enclosing = ASTUtil.findNearestAncestor(loop, IASTFunctionDefinition.class);
		ConstantPropagation constantProp = new ConstantPropagation(enclosing);
		IASTExpression ubExpr = ((IASTBinaryExpression) loop.getConditionExpression()).getOperand2();
		upperBound = constantProp.evaluate(ubExpr);
	}

	@Override
	protected void doLoopFormCheck() {
		IASTStatement body = loop.getBody();
		// If the body is empty, exit out -- pointless to unroll.
		if (body == null || body instanceof IASTNullStatement) {
			status.addFatalError(Messages.UnrollLoopCheck_NothingToUnroll);
			return;
		}

		ForStatementInquisitor loopInquisitor = ForStatementInquisitor.getInquisitor(loop);
		
		// If the loop is not a counted loop, fail
		if (!loopInquisitor.isCountedLoop()) {
			status.addFatalError(Messages.UnrollLoopCheck_LoopFormNotSupported);
			return;
		}

		// If the loop contains unsupported statements, fail
		IASTNode unsupported = loopInquisitor.getFirstUnsupportedStmt();
		if (unsupported != null) {
			status.addFatalError(Messages.UnrollLoopCheck_LoopContainsUnsupported + ASTUtil.toString(unsupported).trim());
			return;
		}

		// If the upper bound is not a constant, we cannot do loop unrolling
		if (upperBound == null) {
			status.addFatalError(Messages.UnrollLoopCheck_UpperBoundNotConstant);
			return;
		}

		// If lower bound is not constant, we can't calculate the number of times to repeat the "trailer"
		// after the unrolled loop
		if (loopInquisitor.getLowerBound() == null) {
			status.addFatalError(Messages.UnrollLoopCheck_UpperBoundNotConstant);
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
			status.addFatalError(Messages.UnrollLoopCheck_IndexVariableChangedInBody);
			return;
		}

	}

	@Override
	protected void doParameterCheck(UnrollLoopParams params) {
		// Check unroll factor validity...
		if (params.getUnrollFactor() <= 0) {
			status.addFatalError(Messages.UnrollLoopCheck_InvalidUnrollFactor);
			return;
		}

		if (upperBound == null) {
			status.addFatalError(Messages.UnrollLoopCheck_CantDetermineUpperBound);
			return;
		}

		// If we are unrolling more than the number of times the loop will
		// run (upper bound - lower bound), we can't do the refactoring.
		ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(loop);
		Long lbo = inq.getLowerBound();
		if(lbo == null){
			status.addFatalError(Messages.UnrollLoopCheck_CantDetermineLowerBound);
			return;
		}
		long lb = lbo.longValue();
		long loopRunTimes = upperBound.longValue() - lb;
		if (params.getUnrollFactor() > loopRunTimes) {
			status.addFatalError(Messages.UnrollLoopCheck_CantUnrollMoreTimesThanLoopRuns);
			return;
		}
	}

	public Long getUpperBound() {
		return upperBound;
	}
}
