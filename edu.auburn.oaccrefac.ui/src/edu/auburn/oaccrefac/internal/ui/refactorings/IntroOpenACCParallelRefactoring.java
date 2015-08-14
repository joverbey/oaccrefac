/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 * 
 * Contributors: 
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 ******************************************************************************/
package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.change.IASTRewrite;
import edu.auburn.oaccrefac.core.dependence.check.DependenceCheck;
import edu.auburn.oaccrefac.core.dependence.check.IntroOpenACCParallelCheck;

/**
 * Refactoring that adds a <code>#pragma acc parallel</code> directive to a for-loop.
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
@SuppressWarnings("restriction")
public class IntroOpenACCParallelRefactoring extends ForLoopRefactoring {

    public IntroOpenACCParallelRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        DependenceCheck check = new IntroOpenACCParallelCheck(getLoop());
        check.check(status, pm);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {
        
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null; // Refactoring history is not supported.
    }
}
