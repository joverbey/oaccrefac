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
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.InquisitorFactory;

public class LoopCuttingCheck extends AbstractStripMineCheck {

    public LoopCuttingCheck(IASTForStatement loop) {
        super(loop);
    }
    
    @Override
    protected void doParameterCheck(RefactoringStatus status, AbstractStripMineParams params) {
    	
    	// Presence of a openacc pragma doesn't influence whether or not loop 
    	// cutting can be performed. This is because for loop cutting to be 
    	// performed, each loop iteration must be independent from another.
    	// If this is the case, then cut sections of the iterations will also 
    	// be independent, meaning they are still parellelizable.
    	
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.loop);
    
        // Check strip factor validity...
        if (params.getNumFactor() <= 0) {
        	status.addFatalError("Invalid cut factor (<= 0).");
        	return;
        }
    
        //Check that iterator is divisible by cut size(new loop iterations = 4)
        //If not then refactoring will not be allowed because 
        //loop behavior would change.
        int iterator = inq.getIterationFactor();
        if (params.getNumFactor() % iterator != 0 || params.getNumFactor() <= iterator) {
        	status.addFatalError(
        		"LoopCut factor must be greater than and "
        				+ "divisible by the intended loop's iteration factor."
        	);
        	return;
        }
    
    }
}
