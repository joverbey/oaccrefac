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
 *     Carl Worley (Auburn) - initial API and implementation
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class StripMineCheck extends ForLoopCheck<StripMineParams> {

    public StripMineCheck(RefactoringStatus status, IASTForStatement loop) {
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
    
    @Override
    protected void doParameterCheck(StripMineParams params) {
    	// Presence of a openacc pragma doesn't influence whether or not strip 
    	// mining can be performed. This is because for strip mining to be 
    	// performed, each loop iteration must be independent from another.
    	// If this is the case, then sections of the iterations will also be 
    	// independent, meaning they are still parellelizable.
    	
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.loop);
        
        // Check strip factor validity...
        if (params.getStripFactor() <= 0) {
            status.addFatalError(Messages.StripMineCheck_InvalidStripFactor);
            return;
        }
        
        if (ASTUtil.isNameInScope(params.getNewNameOuter(), loop.getScope())) {
        	status.addWarning(Messages.StripMineCheck_OuterNameAlreadyExists);
        }
        
        if (params.shouldBeZeroBased() && ASTUtil.isNameInScope(params.getNewNameInner(), loop.getScope())) {
        	status.addWarning(Messages.StripMineCheck_InnerNameAlreadyExists);
        }

        // If the strip factor is not divisible by the original linear
        // iteration factor, (i.e. loop counts by 4), then we cannot
        // strip mine because the refactoring will change behavior
        int iterFactor = inq.getIterationFactor();
        if (params.getStripFactor() % iterFactor != 0 || params.getStripFactor() <= iterFactor) {
            status.addError(Messages.StripMineCheck_FactorMustBeGreaterAndDivisible);
        }
        
        if(!params.shouldHandleOverflow() && inq.getInclusiveUpperBound() % params.getStripFactor() != 0) {
        	status.addWarning(Messages.StripMineCheck_OverflowWillOccur);
        }
        
    }
    
    
}
