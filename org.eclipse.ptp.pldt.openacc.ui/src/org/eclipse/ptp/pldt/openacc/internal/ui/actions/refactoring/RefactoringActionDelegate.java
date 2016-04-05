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
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.ui.actions.refactoring;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.RefactoringRunner;
import org.eclipse.cdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public abstract class RefactoringActionDelegate implements IWorkbenchWindowActionDelegate {

    private IWorkbenchWindow window;
    private ITextSelection textSelection;

    @Override
    public void run(IAction action) {
        // Get the CEditor in order to get the project
        CEditor editor = (CEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .getActiveEditor();

        ISelection selection = editor.getSelectionProvider().getSelection();
        if (selection instanceof ITextSelection) {
            textSelection = (ITextSelection) selection;
        }

        final IWorkingCopy wc = CUIPlugin.getDefault().getWorkingCopyManager().getWorkingCopy(editor.getEditorInput());

        if (wc != null) {
            new RefactoringRunner((ICElement) wc, textSelection, editor.getSite(), wc.getCProject()) {
                @Override
                public void run() {
                    CRefactoring refac = createRefactoring(wc, textSelection, project);
                    run(createWizard(refac), refac, RefactoringSaveHelper.SAVE_NOTHING);
                }
            }.run();
        }
    }

    @Override
	public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof ITextSelection) {
            setSelection((ITextSelection) selection);
        }
    }

    public abstract CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project);

    public abstract RefactoringWizard createWizard(Refactoring refactoring);

    /**
     * We can use this method to dispose of any system resources we previously allocated.
     * 
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    @Override
    public void dispose() {
    }

    /**
     * We will cache window object in order to be able to provide parent shell for the message dialog.
     * 
     * @see IWorkbenchWindowActionDelegate#init
     */
    @Override
    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

    public IWorkbenchWindow getWindow() {
        return window;
    }

    public void setWindow(IWorkbenchWindow w) {
        window = w;
    }

    public ITextSelection getSelection() {
        return textSelection;
    }

    public void setSelection(ITextSelection toSet) {
        textSelection = toSet;
    }

}
