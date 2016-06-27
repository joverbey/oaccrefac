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

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;

public abstract class AbstractTileLoopsAlteration 
	extends ForLoopAlteration<AbstractTileLoopsCheck> {
	
	public AbstractTileLoopsAlteration(IASTRewrite rewriter, AbstractTileLoopsCheck check) {
        super(rewriter, check);
    }
	
	protected String getOperatorAsString(IASTBinaryExpression condExpr) {
		String compOp;
		switch (condExpr.getOperator()) {
		case IASTBinaryExpression.op_lessEqual:
			compOp = "<=";
			break;
		case IASTBinaryExpression.op_lessThan:
			compOp = "<";
			break;
		default:
			throw new IllegalStateException();
		}
		return compOp;
	}
    
}