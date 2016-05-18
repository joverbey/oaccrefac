/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

/**
 * StripMineAlteration defines a loop strip mine refactoring algorithm. Loop strip
 * mining takes a sequential loop and essentially creates 'strips' through perfectly
 * nesting a by-strip loop and an in-strip loop.
 * <p>
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     // ...
 * }
 * </pre>
 * 
 * Refactors to: Refactors to: The outer loop is the by-strip and the inner loop is the
 * in-strip loop...
 * 
 * <pre>
 * for (int i_0 = 0; i_0 < 10; i_0 += 2) {
 *     for (int i = i_0; (i < i_0 + 2 && i < 10); i++) {
 *         // ...
 *     }
 * }
 * </pre>
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
public class StripMineAlteration extends AbstractStripMineAlteration<StripMineCheck> {

    /**
     * Constructor. Takes parameters for strip factor and strip depth to tell the refactoring which perfectly nested
     * loop to strip mine.
     * 
     * @author Adam Eichelkraut
     * @param rewriter
     *            -- rewriter associated with the for loop
     * @param stripFactor
     *            -- factor for how large strips are
     */
    public StripMineAlteration(IASTRewrite rewriter, int stripFactor, String newName, StripMineCheck check) {
        super(rewriter, stripFactor, newName, check);
    }
    
    @Override
    protected String getOuterCond(String newName, String compOp, String ub, int numValue) {
    	return String.format("%s %s %s", newName, compOp, ub);
    }
    
    @Override
    protected String getOuterIter(String newName, int numFactor) {
    	return String.format("%s += %d", newName, numFactor);
    }
    
    @Override
    protected String getInnerCond(String indexVar, String newName, int numFactor, 
    		String compOp, String ub) {
    	return parenth(String.format("%s <  %s + %d && %s %s %s", indexVar, newName, numFactor, indexVar, compOp, ub));
    }
    
    @Override
    protected String getInnerIter(IASTForStatement loop, String indexVar, String ub, int numValue) {
    	return loop.getIterationExpression().getRawSignature();
    }



	

    
    
    

    

    

}
