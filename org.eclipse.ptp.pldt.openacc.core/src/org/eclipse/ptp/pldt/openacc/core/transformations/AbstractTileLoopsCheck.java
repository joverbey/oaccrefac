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

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public abstract class AbstractTileLoopsCheck extends ForLoopCheck<AbstractTileLoopsParams> {
	
	public AbstractTileLoopsCheck(RefactoringStatus status, IASTForStatement loop) {
        super(status, loop);
    }
	
	@Override 
	protected void doLoopFormCheck() {
		ForStatementInquisitor inquisitor = ForStatementInquisitor.getInquisitor(loop);
		if (!inquisitor.isCountedLoop()) {
            status.addFatalError(Messages.AbstractTileLoopsCheck_LoopFormNotSupported);
            return;
        }
	}
}