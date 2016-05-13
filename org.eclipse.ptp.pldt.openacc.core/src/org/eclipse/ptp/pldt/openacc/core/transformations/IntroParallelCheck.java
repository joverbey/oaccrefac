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
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.InquisitorFactory;

public class IntroParallelCheck extends ForLoopCheck<RefactoringParams> {

	protected boolean parentPragma = false;

	public IntroParallelCheck(final IASTForStatement loop) {
		super(loop);
	}

	public boolean getParentPragma() {
		return parentPragma;
	}

	private void setParentPragma(boolean in) {
		parentPragma = in;
	}

	@Override
	protected void doLoopFormCheck(RefactoringStatus status) {
		checkPragma(status);
		checkParentPragma(status);
	}

	@Override
	public void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
		if (dep != null && dep.hasLevel1CarriedDependence()) {
			status.addError("This loop cannot be parallelized because it carries a dependence.");
		}

	}

	private void checkPragma(RefactoringStatus status) {
		ForStatementInquisitor loop1 = InquisitorFactory.getInquisitor(loop);
		if (loop1.getPragmas().length != 0) {
			status.addFatalError("When a loop has a pragma associated with it, it cannot have another pragma added to it.");
		}
	}

	private void checkParentPragma(RefactoringStatus status) {
		IASTNode node = loop.getParent();
		while (node != null)
			if (node instanceof IASTForStatement) {
				IASTForStatement forStat = (IASTForStatement) node;
				if (ASTUtil.getPragmas(forStat).length != 0) {
					setParentPragma(true);
					status.addError("When a loop has a parent with a pragma associated with it, it cannot parallelized.");
					break;
				}
				else{
					node = node.getParent();
				}
			}
				else if(node.getParent() != null){
					node = node.getParent();
				}
				else {
					break;
				}
	}

}

// boolean checkBreak(RefactoringStatus status){
// ForStatementInquisitor loop1 = InquisitorFactory.getInquisitor(loop);
// IASTStatement body = loop.getBody();
// if( body ){
//
// }
// return true;
// }
