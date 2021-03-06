/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.actions.refactoring;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.LoopRefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.NumberInputComposite.NumberValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.UnrollLoopRefactoring;

@SuppressWarnings("restriction")
public class UnrollLoopDelegate extends RefactoringActionDelegate {
    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc,
            ITextSelection selection, ICProject project) {
        return new UnrollLoopRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        final UnrollLoopRefactoring refac = (UnrollLoopRefactoring) refactoring;
        LoopRefactoringWizard wizard = new LoopRefactoringWizard(refactoring, Messages.UnrollLoopDelegate_WizardTitle, true); 
        wizard.getInputPage().addInputControl(Messages.UnrollLoopDelegate_UnrollFactorLabel, new NumberValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setUnrollFactor(value);
            }
        });
        return wizard;
    }
}
