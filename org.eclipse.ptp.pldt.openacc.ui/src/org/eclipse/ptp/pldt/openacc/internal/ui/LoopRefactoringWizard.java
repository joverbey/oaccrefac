/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Zack King (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui;

import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.TileLoopsRefactoring;

public class LoopRefactoringWizard extends RefactoringWizard {

    private LoopRefactoringWizardPage inputPage;

    public LoopRefactoringWizard(Refactoring refactoring, String title) {
        super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
        
        setDefaultPageTitle(title);
        setDialogSettings(CUIPlugin.getDefault().getDialogSettings());

        inputPage = new LoopRefactoringWizardPage(title);
    }

    @Override
    protected void addUserInputPages() {
        addPage(inputPage);
    }

    public LoopRefactoringWizardPage getInputPage() {
        return inputPage;
    }
    
    public ColumnedLoopRefactoringWizardPage getColumnPage() {
    	inputPage = new ColumnedLoopRefactoringWizardPage(getDefaultPageTitle(), (TileLoopsRefactoring) getRefactoring());
    	return (ColumnedLoopRefactoringWizardPage) inputPage;
    }
}
