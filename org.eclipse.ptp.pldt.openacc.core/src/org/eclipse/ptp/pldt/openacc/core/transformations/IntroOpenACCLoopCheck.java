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
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;

public class IntroOpenACCLoopCheck extends ForLoopCheck<RefactoringParams> {

	protected boolean parentPragma = false;
	private boolean kernels;

	public IntroOpenACCLoopCheck(final IASTForStatement loop, boolean kernels) {
		super(loop);
		this.kernels = kernels;
	}

	public boolean getParentPragma() {
		return parentPragma;
	}

	private void setParentPragma(boolean in) {
		parentPragma = in;
	}

	@Override
	protected void doLoopFormCheck(RefactoringStatus status) {
		if (kernels) {
			checkParallel(status);
	    	if (!ASTUtil.getPragmaNodes(loop).isEmpty()) {
	            status.addError("This loop contains an ACC pragma.");
	        }
		} else {
			checkPragma(status);
			checkParentPragma(status);
			checkKernel(status);
		}
	}

	@Override
	public void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
		if (dep != null && dep.hasLevel1CarriedDependence()) {
			status.addError("This loop cannot be parallelized because it carries a dependence.");
		}

	}

	private void checkPragma(RefactoringStatus status) {
		if (!ASTUtil.getPragmaNodes(loop).isEmpty()) {
			status.addFatalError("When a loop has a pragma associated with it, it cannot have another pragma added to it.");
		}
	}

	private void checkParentPragma(RefactoringStatus status) {
		IASTNode node = loop.getParent();
		while (node != null)
			if (node instanceof IASTStatement) {
				IASTStatement stat = (IASTStatement) node;
				if (OpenACCUtil.isAccConstruct(stat, ASTAccParallelNode.class)
						|| OpenACCUtil.isAccConstruct(stat, ASTAccParallelLoopNode.class)) {
					setParentPragma(true);
					status.addError("When a loop has a parent with a parallel pragma associated with it, it cannot parallelized.");
					break;
				} else {
					node = node.getParent();
				}
			} else if (node.getParent() != null) {
				node = node.getParent();
			} else {
				break;
			}
	}

	private void checkKernel(RefactoringStatus status) {
		IASTNode node = loop.getParent();
		IASTStatement stat = (IASTStatement) node;
		while (stat != null) {
			if (OpenACCUtil.isAccConstruct(stat, ASTAccKernelsNode.class)
					|| OpenACCUtil.isAccConstruct(stat, ASTAccKernelsLoopNode.class)) {
				status.addFatalError("When a loop has a parent with a kernel pragma associated with it, it cannot parallelized.");
				break;
			} else if (stat.getParent() instanceof IASTStatement) {
					stat = (IASTStatement) stat.getParent();
			}
			else if (stat.getParent() instanceof IASTFunctionDefinition){
				break;
			}
			else {
				break;
			}

		}
	}
	
	private void checkParallel(RefactoringStatus status) {
		IASTNode node = loop.getParent();
		IASTStatement stat = (IASTStatement) node;
		while (stat != null) {
			if (OpenACCUtil.isAccConstruct(stat, ASTAccParallelNode.class)
					|| OpenACCUtil.isAccConstruct(stat, ASTAccParallelLoopNode.class)) {
				status.addFatalError("When a loop has a parent with a parallel pragma associated with it, it cannot kernelized.");
				break;
			} else if (stat.getParent() instanceof IASTStatement) {
					stat = (IASTStatement) stat.getParent();
			}
			else if (stat.getParent() instanceof IASTFunctionDefinition){
				break;
			}
			else {
				break;
			}

		}
	}

}
