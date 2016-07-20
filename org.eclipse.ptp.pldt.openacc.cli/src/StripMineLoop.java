/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *     Jacob Allen Neeley (Auburn) - initial API and implementation
 *******************************************************************************/

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StripMineAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StripMineCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.StripMineParams;

/**
 * StripMine performs the strip mine refactoring.
 */
public class StripMineLoop extends CLILoopRefactoring<StripMineParams, StripMineCheck> {

	private int stripFactor;
	private boolean zeroBased;
	private boolean handleOverflow;
    private String newNameOuter;
    private String newNameInner;
    
    public StripMineLoop(int stripFactor, boolean zeroBased, boolean handleOverflow, String newNameOuter,
			String newNameInner) {
		this.stripFactor = stripFactor;
		this.zeroBased = zeroBased;
		this.handleOverflow = handleOverflow;
		this.newNameOuter = newNameOuter;
		this.newNameInner = newNameInner;
	}

	@Override
    public StripMineCheck createCheck(IASTStatement loop) {
		 return new StripMineCheck(new RefactoringStatus(), (IASTForStatement) loop);
    }

    @Override
    protected StripMineParams createParams(IASTStatement forLoop) {
        return new StripMineParams(stripFactor, zeroBased, handleOverflow, newNameOuter, newNameInner);
    }

    @Override
	public StripMineAlteration createAlteration(IASTRewrite rewriter, 
    		StripMineCheck check) throws CoreException {
        return new StripMineAlteration(rewriter, stripFactor, zeroBased, handleOverflow, newNameOuter, newNameInner, check);
    }

}
