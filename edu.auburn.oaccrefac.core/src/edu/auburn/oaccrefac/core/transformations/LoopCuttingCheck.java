/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Jacob Neeley (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class LoopCuttingCheck extends ForLoopCheck<LoopCuttingParams> {

    public LoopCuttingCheck(IASTForStatement loop) {
        super(loop);
    }
    
    @Override
    protected void doParameterCheck(RefactoringStatus status, LoopCuttingParams params) {
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.loop);
    
 // Check strip factor validity...
    if (params.getCutFactor() <= 0) {
        status.addFatalError("Invalid cut factor (<= 0).");
        return;
    }
    
 //Check that iterator is divisible by cut size(new loop iterations = 4)
 //If not then refactoring will not be allowed because 
 //loop behavior would change.
    int iterator = inq.getIterationFactor();
    if (params.getCutFactor() % iterator != 0 || params.getCutFactor() <= iterator) {
        status.addFatalError("LoopCut factor must be greater than and "
                + "divisible by the intended loop's iteration factor.");
        return;
    }
    
    }
}
