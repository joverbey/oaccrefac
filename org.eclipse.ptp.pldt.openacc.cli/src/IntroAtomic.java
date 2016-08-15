/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IntroAtomicAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IntroAtomicCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.SourceAlteration;

public class IntroAtomic extends CLISourceStatementsRefactoring<RefactoringParams, IntroAtomicCheck> {

	@Override
	protected IntroAtomicCheck createCheck(IASTStatement statement) {
		return new IntroAtomicCheck(new RefactoringStatus(), getStatements(), getAllEnclosedNodes());
	}

	@Override
	public SourceAlteration<?> createAlteration(IASTRewrite rewriter, IntroAtomicCheck check) throws CoreException {
		return new IntroAtomicAlteration(rewriter, check);
	}

}