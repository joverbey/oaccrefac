/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.FunctionGraphException;
import org.eclipse.ptp.pldt.openacc.internal.core.FunctionNode;

public class IntroRoutineCheck extends SourceStatementsCheck<RefactoringParams> {

	private FunctionNode graph;
	
	public IntroRoutineCheck(RefactoringStatus status, IASTStatement[] statements, IASTNode[] allEnclosedNodes) {
		super(status, statements, allEnclosedNodes);
	}
	
	@Override
	public RefactoringStatus doCheck(IProgressMonitor pm) {
		List<IASTFunctionDefinition> definitions = new ArrayList<IASTFunctionDefinition>();
		for (IASTNode node : getAllEnclosedNodes()) {
			if (node instanceof IASTFunctionDefinition) {
				definitions.add((IASTFunctionDefinition) node);
			}
		}
		for (IASTStatement statement : getStatements()) {
			for (IASTFunctionCallExpression call : ASTUtil.find(statement, IASTFunctionCallExpression.class)) {
				IASTFunctionDefinition definition = ASTUtil.findFunctionDefinition(call);
				if (definition == null && !(definitions.contains(statement))) {
					status.addError(Messages.IntroRoutineCheck_CannotFindFunctionDefinition, ASTUtil.getStatusContext(call, call));
				} else {
				    definitions.add(definition);
				}
			}
		}
		try {
			graph = new FunctionNode(definitions, status);
		} catch (FunctionGraphException e) {
			status.addFatalError(e.getMessage(), e.getContext());
		}
		//TODO: Check whether root level is valid.
		return status;
	}
	
	public FunctionNode getGraph() {
		return graph;
	}
	
}
