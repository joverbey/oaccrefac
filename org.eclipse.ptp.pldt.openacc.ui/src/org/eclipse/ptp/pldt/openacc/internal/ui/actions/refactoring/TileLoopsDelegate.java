/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
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
import org.eclipse.ptp.pldt.openacc.internal.ui.ColumnedLoopRefactoringWizardPage;
import org.eclipse.ptp.pldt.openacc.internal.ui.LoopRefactoringWizard;
import org.eclipse.ptp.pldt.openacc.internal.ui.NumberInputComposite.NumberValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.StringInputComposite.StringValueChangedListener;
import org.eclipse.ptp.pldt.openacc.internal.ui.refactorings.TileLoopsRefactoring;

@SuppressWarnings("restriction")
public class TileLoopsDelegate extends RefactoringActionDelegate {

    @Override
    public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
        return new TileLoopsRefactoring(wc, selection, project);
    }

    @Override
    public RefactoringWizard createWizard(Refactoring refactoring) {
        final TileLoopsRefactoring refac = (TileLoopsRefactoring) refactoring;
        LoopRefactoringWizard wizard = new LoopRefactoringWizard(refactoring, Messages.TileLoopsDelegate_TileLoops, true);
        ColumnedLoopRefactoringWizardPage page = wizard.getColumnPage();
        page.addLeftInputControl(Messages.TileLoopsDelegate_TileWidth, new NumberValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setWidth(value);
            }
        });
        page.addLeftInputControl(Messages.TileLoopsDelegate_TileHeight, new NumberValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setHeight(value);
            }
        });
        page.addLeftInputControl(Messages.TileLoopsDelegate_InnerIndexVariable, new StringValueChangedListener() {
            @Override
            public void stringValueChanged(String value) {
                refac.setInnerNewName(value);
            }
        });
        page.addLeftInputControl(Messages.TileLoopsDelegate_OuterIndexVariable, new StringValueChangedListener() {
            @Override
            public void stringValueChanged(String value) {
                refac.setOuterNewName(value);
            }
        });
        
        page.addRightInputControl(Messages.TileLoopsDelegate_Stride, new NumberValueChangedListener() {
            @Override
            public void valueChanged(int value) {
                refac.setStride(value);
            }
        });
        page.addRightInputControl(Messages.TileLoopsDelegate_IndexVariableName, new StringValueChangedListener() {
            @Override
            public void stringValueChanged(String value) {
                refac.setNewName(value);
            }
        });
        return wizard;
    }
}
