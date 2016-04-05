/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.auburn.oaccrefac.internal.ui.refactorings.IntroduceKernelsLoopRefactoring;

/**
 * Our action implements workbench action delegate. The action proxy will be created by the workbench and shown in the
 * UI. When the user tries to use the action, this delegate will be created and execution will be delegated to it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
@SuppressWarnings("restriction")
public class IntroduceKernelsLoopDelegate extends RefactoringActionDelegate {

    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new IntroduceKernelsLoopRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        return new Wizard(refactoring);
    }

    private static class Wizard extends RefactoringWizard {

        public Wizard(Refactoring refactoring) {
            super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
            setDefaultPageTitle("Introduce OpenACC Kernels Loop");
            setDialogSettings(CUIPlugin.getDefault().getDialogSettings());
        }

        @Override
        protected void addUserInputPages() {
        }
    }
}
