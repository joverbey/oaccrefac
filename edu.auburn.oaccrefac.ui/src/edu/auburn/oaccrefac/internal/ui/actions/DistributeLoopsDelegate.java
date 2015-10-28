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
package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.auburn.oaccrefac.internal.ui.LoopRefactoringWizard;
import edu.auburn.oaccrefac.internal.ui.refactorings.DistributeLoopsRefactoring;

@SuppressWarnings("restriction")
public class DistributeLoopsDelegate extends RefactoringActionDelegate {
    
    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new DistributeLoopsRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        if (!(refactoring instanceof DistributeLoopsRefactoring))
            throw new ClassCastException("Refactoring not LoopFissionRefactoring!");
        LoopRefactoringWizard tomRiddle = 
                new LoopRefactoringWizard(refactoring, "LoopFissionRefactoring");
        return tomRiddle;
    }
}
