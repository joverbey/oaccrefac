/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.actions.refactoring;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.LoopRefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.LoopRefactoringWizardPage;
import org.eclipse.ptp.pldt.openacc.internal.ui.NumberInputComposite.NumberValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.StringInputComposite.StringValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.StripMineLoopRefactoring;

@SuppressWarnings("restriction")
public class StripMineLoopDelegate extends RefactoringActionDelegate {

    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new StripMineLoopRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        final StripMineLoopRefactoring refac = (StripMineLoopRefactoring) refactoring;
        LoopRefactoringWizard wizard = new LoopRefactoringWizard(refactoring, "Strip Mine Loop");
        LoopRefactoringWizardPage page = wizard.getInputPage();
        page.addInputControl("Strip Size", new NumberValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setStripFactor(value);
            }
        });
        page.addInputControl("Index Variable Name", new StringValueChangedListener() {
            @Override
            public void stringValueChanged(String value) {
                refac.setNewName(value);
            }
        });
        page.setButton(true, refac);
        return wizard;
    }

}
