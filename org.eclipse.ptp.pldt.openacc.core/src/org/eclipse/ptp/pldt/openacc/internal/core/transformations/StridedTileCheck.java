/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Jacob Allen Neeley (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DependenceAnalysis;

public class StridedTileCheck extends AbstractTileLoopsCheck {

    public StridedTileCheck(RefactoringStatus status, IASTForStatement loop) {
        super(status, loop);
    }
    
    @Override
    protected void doParameterCheck(AbstractTileLoopsParams params) {
    	
    	// Presence of a openacc pragma doesn't influence whether or not loop 
    	// cutting can be performed. This is because for loop cutting to be 
    	// performed, each loop iteration must be independent from another.
    	// If this is the case, then cut sections of the iterations will also 
    	// be independent, meaning they are still parellelizable.
    	
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.loop);
    
        // Check strip factor validity...
        if (params.getNumFactor() <= 0) {
        	status.addFatalError(Messages.StridedTileCheck_InvalidCutFactor);
        	return;
        }
        
        if (ASTUtil.isNameInScope(params.getNewName(), loop.getScope())) {
        	status.addWarning(Messages.StridedTileCheck_NameAlreadyExists);
        }
    
        //Check that iterator is divisible by cut size(new loop iterations = 4)
        //If not then refactoring will not be allowed because 
        //loop behavior would change.
        int iterator = inq.getIterationFactor();
        if (params.getNumFactor() % iterator != 0 || params.getNumFactor() <= iterator) {
        	status.addFatalError(
        		Messages.StridedTileCheck_FactorMustBeGreater
        				+ Messages.StridedTileCheck_DivisibleByIterationFactor
        	);
        	return;
        }
    
    }
    
    @Override
	public void doDependenceCheck(DependenceAnalysis dep) {
		if (dep != null && dep.hasLevel1CarriedDependence()) {
			status.addError(Messages.StridedTileCheck_CannotCutCarriesDependence);
		}

	}
}
