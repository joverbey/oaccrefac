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

public class StripMineCheck extends AbstractTileLoopsCheck {

    public StripMineCheck(RefactoringStatus status, IASTForStatement loop) {
        super(status, loop);
    }

    @Override
    protected void doParameterCheck(AbstractTileLoopsParams params) {
    	
    	// Presence of a openacc pragma doesn't influence whether or not strip 
    	// mining can be performed. This is because for strip mining to be 
    	// performed, each loop iteration must be independent from another.
    	// If this is the case, then sections of the iterations will also be 
    	// independent, meaning they are still parellelizable.
    	
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.loop);
        
        // Check strip factor validity...
        if (params.getNumFactor() <= 0) {
            status.addFatalError("Invalid strip factor (<= 0).");
            return;
        }
        
        if (ASTUtil.isNameInScope(params.getNewName(), loop.getScope())) {
        	status.addWarning("Index variable name already exists in scope.");
        }

        // If the strip factor is not divisible by the original linear
        // iteration factor, (i.e. loop counts by 4), then we cannot
        // strip mine because the refactoring will change behavior
        int iterator = inq.getIterationFactor();
        if (params.getNumFactor() % iterator != 0 || params.getNumFactor() <= iterator) {
            status.addError("Strip mine factor must be greater than and "
                    + "divisible by the intended loop's iteration factor.");
            return;
        }
        
    }
    
    
}
