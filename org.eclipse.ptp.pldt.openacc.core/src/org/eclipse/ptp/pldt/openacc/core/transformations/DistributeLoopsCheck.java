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

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DataDependence;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceType;
import org.eclipse.ptp.pldt.openacc.core.dependence.Direction;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class DistributeLoopsCheck extends ForLoopCheck<RefactoringParams> {

	public DistributeLoopsCheck(IASTForStatement loop) {
		super(loop);
	}

	@Override
	public void doLoopFormCheck(RefactoringStatus status) {
		// If the loop doesn't have children, bail.
		if (!(loop.getBody() instanceof IASTCompoundStatement)) {
			status.addFatalError("Loop body is not a compound statement, so distribution cannot be performed.");
			return;
		}

		if (loop.getBody().getChildren().length < 2) {
			status.addFatalError("Loop distribution can only be applied if there is more than one statement in the loop body.");
			return;
		}
		
		if (!ASTUtil.find(loop.getBody(), IASTDeclarationStatement.class).isEmpty()) {
    		status.addError("Loop distribution isolates declaration statement.");
    	}
		checkPragma(status);
	}

	@Override
	protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
		for (DataDependence d : dep.getDependences()) {
			// if there is a loop-carried anti-dependence in the less-than direction
			if (d.isLoopCarried()
					&& (d.getDirectionVector()[d.getLevel() - 1] == Direction.LT
							|| d.getDirectionVector()[d.getLevel() - 1] == Direction.LE)
					&& d.getType() == DependenceType.ANTI) {

				status.addError("Distribution cannot be performed because the loop carries a dependence.");
			}
		}
	}

	private void checkPragma(RefactoringStatus status) {
		if (!ASTUtil.getPragmaNodes(loop).isEmpty()) {
			status.addFatalError("Can't distribute loop with pragmas.");
		}
	}
	
	

}
