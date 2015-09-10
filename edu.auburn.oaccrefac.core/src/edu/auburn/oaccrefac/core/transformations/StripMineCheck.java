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
package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class StripMineCheck extends ForLoopCheck<StripMineParams> {

    public StripMineCheck(IASTForStatement loop) {
        super(loop);
    }

    @Override
    protected void doParameterCheck(RefactoringStatus status, StripMineParams params) {
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.loop);

        // Check strip factor validity...
        if (params.getStripFactor() <= 0) {
            status.addFatalError("Invalid strip factor (<= 0).");
            return;
        }

        // Check depth validity...
//        if (params.getDepth() < 0 || params.getDepth() >= inq.getPerfectLoopNestHeaders().size()) {
//            status.addFatalError("There is no for-loop at depth " + params.getDepth());
//            return;
//        }

        // If the strip factor is not divisible by the original linear
        // iteration factor, (i.e. loop counts by 4), then we cannot
        // strip mine because the refactoring will change behavior
        int iterator = inq.getIterationFactor();
        if (params.getStripFactor() % iterator != 0 || params.getStripFactor() <= iterator) {
            status.addFatalError("Strip mine factor must be greater than and "
                    + "divisible by the intended loop's iteration factor.");
            return;
        }
    }
    
    
}
