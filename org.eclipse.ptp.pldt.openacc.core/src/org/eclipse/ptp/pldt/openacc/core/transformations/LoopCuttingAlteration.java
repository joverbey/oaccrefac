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

/**
 * The LoopCuttingAlteration refactoring cuts a for loop into multiple
 * loops.
 * <p>
 * 
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 15; i++)
 *     // ...
 * </pre>
 * 
 * Refactors to: The outer loop is the by-strip and the inner loop is the
 * in-strip loop...
 * 
 * <pre>
 * for (int set = 0; set < 3, set++)
 *     for (int i = set; i < 15, i+=3)
 *          // ...
 * </pre>
 *          
 * @author Jeff Overbey
 * @author Jacob Neeley
 */
public class LoopCuttingAlteration extends AbstractStripMineAlteration<LoopCuttingCheck> {
    
    public LoopCuttingAlteration(IASTRewrite rewriter, int cutFactorIn, String newNameIn, LoopCuttingCheck check) {
        super(rewriter, cutFactorIn, newNameIn, check);
    }
    
    @Override
    protected String getOuterCond(String newName, String compOp, String ub, int numValue) {
    	return String.format("%s < %s", newName, ub + " / " + numValue);
    }
    
    @Override
    protected String getOuterIter(String newName, int numFactor) {
    	return newName + "++";
    }
    
    @Override
    protected String getInnerCond(String indexVar, String newName, int numFactor, 
    		String compOp, String ub) {
    	return parenth(String.format("%s < %s", indexVar, ub));
    }
    
    @Override
    protected String getInnerIter(IASTForStatement loop, String indexVar, String ub, int numValue) {
    	return indexVar + "+=" + ub + "/" + numValue;
    }
    
}
