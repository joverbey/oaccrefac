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
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class StripMineCheck extends ForLoopCheck<StripMineParams> {

    public StripMineCheck(IASTForStatement loop) {
        super(loop);
    }

    @Override
    protected void doParameterCheck(RefactoringStatus status, StripMineParams params) {
    	
    	// Presence of a openacc pragma doesn't influence whether or not strip 
    	// mining can be performed. This is because for strip mining to be 
    	// performed, each loop iteration must be independent from another.
    	// If this is the case, then sections of the iterations will also be 
    	// independent, meaning they are still parellelizable.
    	
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.loop);
        
        // Check strip factor validity...
        if (params.getStripFactor() <= 0) {
            status.addFatalError("Invalid strip factor (<= 0)");
            return;
        }
        
        if (ASTUtil.isNameInScope(params.getNewNameOuter(), loop.getScope())) {
        	status.addWarning("Outer index variable name already exists in scope");
        }
        
        if (params.shouldBeZeroBased() && ASTUtil.isNameInScope(params.getNewNameInner(), loop.getScope())) {
        	status.addWarning("Inner index variable name already exists in scope");
        }

        // If the strip factor is not divisible by the original linear
        // iteration factor, (i.e. loop counts by 4), then we cannot
        // strip mine because the refactoring will change behavior
        int iterFactor = inq.getIterationFactor();
        if (params.getStripFactor() % iterFactor != 0 || params.getStripFactor() <= iterFactor) {
            status.addError("Strip mine factor must be greater than and divisible by the intended loop's iteration factor");
        }
        
        if(!params.shouldHandleOverflow() && inq.getInclusiveUpperBound() % params.getStripFactor() != 0) {
        	status.addWarning("Loop range overflow will not be handled, but the loop upper bound is not divisible by strip factor, so overflow will occur");
        }
        
    }
    
    
}
