/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.NullAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.NullCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.NullParams;

/**
 * NullRefactoring is performs a dependence analysis but makes no changes to the source code. It is intended for
 * testing, since it will refuse to refactor any code that cannot be analyzed.
 */
public class NullRefactoring extends ForLoopRefactoring {

    private NullCheck check;

    public NullRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
		String msg = NLS.bind(Messages.NullRefactoring_SelectedLoopInfo,
				new Object[] { getLoop().getFileLocation().getStartingLineNumber(), ASTUtil.summarize(getLoop()) });
        status.addInfo(msg, getLocation(getLoop()));

        check = new NullCheck(status, getLoop());
        check.performChecks(pm, new NullParams());
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new NullAlteration(rewriter, check).change();
    }
}
