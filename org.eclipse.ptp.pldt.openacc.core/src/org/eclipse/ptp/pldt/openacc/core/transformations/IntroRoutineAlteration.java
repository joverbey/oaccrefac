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

package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.HashSet;

import org.eclipse.ptp.pldt.openacc.internal.core.FunctionNode;

public class IntroRoutineAlteration extends SourceStatementsAlteration<IntroRoutineCheck> {

	private final FunctionNode graph;
	
	public IntroRoutineAlteration(IASTRewrite rewriter, IntroRoutineCheck check) {
		super(rewriter, check);
		graph = check.getGraph();
	}

	@Override
	protected void doChange() {
		HashSet<FunctionNode> marked = new HashSet<FunctionNode>();
		for (FunctionNode node : graph.getChildren()) {
			changeNode(node, marked);
		}
		finalizeChanges();
	}
	
	private void changeNode(FunctionNode node, HashSet<FunctionNode> marked) {
		if (marked.contains(node)) {
			return;
		}
		marked.add(node);
		int offset = node.getDefinition().getFileLocation().getNodeOffset();
        String pragma = pragma("acc routine"); //$NON-NLS-1$
        pragma += (" " + node.getLevel().toString()); //$NON-NLS-1$
        insert(offset, pragma + System.lineSeparator());
        for (FunctionNode child : node.getChildren()) {
        	changeNode(child, marked);
        }
	}

}
