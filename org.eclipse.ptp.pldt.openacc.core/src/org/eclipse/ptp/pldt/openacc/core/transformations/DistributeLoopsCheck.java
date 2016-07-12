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
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;

public class DistributeLoopsCheck extends ForLoopCheck<RefactoringParams> {

	public DistributeLoopsCheck(RefactoringStatus status, IASTForStatement loop) {
		super(status, loop);
	}

	@Override
	public void doLoopFormCheck() {
		// If the loop doesn't have children, bail.
		if (!(loop.getBody() instanceof IASTCompoundStatement)) {
			status.addFatalError(Messages.DistributeLoopsCheck_BodyNotCompound);
			return;
		}

		if (loop.getBody().getChildren().length < 2) {
			status.addFatalError(Messages.DistributeLoopsCheck_OnlyOneStatement);
			return;
		}
		
		if (!ASTUtil.find(loop.getBody(), IASTDeclarationStatement.class).isEmpty()) {
    		status.addError(Messages.DistributeLoopsCheck_IsolatesDeclarationStatement);
    	}
		
		if (OpenACCUtil.isAccConstruct(getLoop())) {
			status.addError(Messages.DistributeLoopsCheck_LoopIsAccConstruct);
		}
	}

	@Override
	protected void doDependenceCheck(DependenceAnalysis dep) {
		for (DataDependence d : dep.getDependences()) {
			// if there is a loop-carried anti-dependence in the less-than direction
			if (d.isLoopCarried()
					&& (d.getDirectionVector()[d.getLevel() - 1] == Direction.LT
							|| d.getDirectionVector()[d.getLevel() - 1] == Direction.LE)
					&& d.getType() == DependenceType.ANTI) {

				status.addError(Messages.DistributeLoopsCheck_LoopCarriesDependence);
			}
		}
	}
	
	

}
