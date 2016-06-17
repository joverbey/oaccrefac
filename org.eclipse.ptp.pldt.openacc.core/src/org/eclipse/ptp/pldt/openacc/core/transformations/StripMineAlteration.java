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

/**
 * StripMineAlteration defines a loop strip mine refactoring algorithm. Loop strip
 * mining takes a sequential loop and essentially creates 'strips' through perfectly
 * nesting a by-strip loop and an in-strip loop.
 * <p>
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 * 	// ...
 * }
 * </pre>
 * 
 * Refactors to: Refactors to: The outer loop is the by-strip and the inner loop is the
 * in-strip loop...
 * 
 * <pre>
 * for (int i_0 = 0; i_0 < 10; i_0 += 2) {
 * 	for (int i = i_0; (i < i_0 + 2 && i < 10); i++) {
 * 		// ...
 * 	}
 * }
 * </pre>
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
public class StripMineAlteration extends ForLoopAlteration<StripMineCheck> {

	private ForLoopAlteration<StripMineCheck> alteration;

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
	public StripMineAlteration(IASTRewrite rewriter, int stripFactor, boolean zeroBased, boolean handleOverflow,
			String newNameOuter, String newNameInner, StripMineCheck check) {
		super(rewriter, check);
		
		if(zeroBased && (newNameInner == null || newNameInner.equals(""))) {
			throw new IllegalStateException();
		}
		
		if(zeroBased) {
			alteration = new ZeroBasedStripMine(rewriter, stripFactor, handleOverflow, newNameOuter, newNameInner, check);
		}
		else {
			alteration = new NormalStripMine(rewriter, stripFactor, handleOverflow, newNameOuter, check);
		}
	}
	
	@Override
	protected void doChange() {
		alteration.change();
	}

}
