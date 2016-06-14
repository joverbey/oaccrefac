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
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.InterchangeLoopsRefactoring;

@SuppressWarnings("restriction")
public class InterchangeLoopsDelegate extends RefactoringActionDelegate {

    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new InterchangeLoopsRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        LoopRefactoringWizard wizard = new LoopRefactoringWizard(refactoring, "Interchange Loops");
        final InterchangeLoopsRefactoring refac = (InterchangeLoopsRefactoring) refactoring;
        wizard.getInputPage().addInputControl("Interchange Depth: ", new NumberValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setExchangeDepth(value);
            }
        });
        return wizard;
    }

}
