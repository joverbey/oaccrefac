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
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IntroRoutineAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IntroRoutineCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.SourceAlteration;

public class IntroRoutine extends CLISourceStatementsRefactoring<RefactoringParams, IntroRoutineCheck> {

	@Override
	protected IntroRoutineCheck createCheck(IASTStatement statement) {
		return new IntroRoutineCheck(new RefactoringStatus(), getStatements(), getAllEnclosedNodes());
	}

	@Override
	public SourceAlteration<?> createAlteration(IASTRewrite rewriter, IntroRoutineCheck check) throws CoreException {
		return new IntroRoutineAlteration(rewriter, check);
	}

}
