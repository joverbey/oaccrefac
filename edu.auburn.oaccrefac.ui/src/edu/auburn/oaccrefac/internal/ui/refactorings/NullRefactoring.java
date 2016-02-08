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
package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.NullAlteration;
import edu.auburn.oaccrefac.core.transformations.NullCheck;
import edu.auburn.oaccrefac.core.transformations.NullParams;

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
        check = new NullCheck(getLoop());
        check.performChecks(status, pm, new NullParams());
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new NullAlteration(rewriter, check).change();
    }

}