/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;

public class ExpandDataConstructAlteration extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

	private PragmaDirectiveAlteration<ExpandDataConstructCheck> alteration;
	
	public ExpandDataConstructAlteration(IASTRewrite rewriter, ExpandDataConstructCheck check) {
		super(rewriter, check);
		IASTForStatement forParent = null;
		IASTNode parent = getStatement().getParent();
		if(parent instanceof IASTForStatement && ((IASTForStatement) parent).getBody().equals(getStatement())) {
			forParent = (IASTForStatement) parent;
		} 
		else if (parent instanceof IASTCompoundStatement && parent.getParent() instanceof IASTForStatement
				&& ((IASTCompoundStatement) parent).getChildren().length == 1) {
			forParent = (IASTForStatement) parent.getParent();
		}
		if(forParent != null) {
			alteration = new Bloat(rewriter, check, forParent);
		}
		else {
			alteration = new Expand(rewriter, check);
		}
	}

	@Override
	protected void doChange() throws Exception {
		/**
		 * This works because we're delegating all action to a single alteration.
		 * If we were to do work in each of multiple alterations, each alteration
		 * would track its own separate set of replacements. If each alteration 
		 * calls its own finalizeChanges(), I think there would be overlap 
		 * errors, etc., from multiple IASTRewrite changes or something. Otherwise, 
		 * if finalizeChanges() is only called once, that call can only see the 
		 * replacements from its own alteration, so the others are simply ignored. 
		 */
		alteration.change();
	}
	
}
