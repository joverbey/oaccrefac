/*******************************************************************************
 * Copyright (c) 2005, 2011 Wind River Systems, Inc. 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Schorn - initial API and implementation
 *     Sergey Prigogin (Google)
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.ui.handlers;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.IntroOpenACCParallelRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.RefactoringRunner;
import org.eclipse.cdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.refactoring.actions.RefactoringAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ColumnLayout;

/**
 * Launches an Extract Local Variable refactoring.
 * @since 5.9
 * @noextend This class is not intended to be subclassed by clients.
 */          
@SuppressWarnings("restriction")
public class IntroOpenACCParallelAction extends RefactoringAction {
    
    public IntroOpenACCParallelAction() {
        super("Introduce OpenACC Parallel");
    }
    
	@Override
	public void run(IShellProvider shellProvider, ICElement elem) {
	}

	@Override
	public void run(IShellProvider shellProvider, IWorkingCopy wc, final ITextSelection sel) {
		if (wc.getResource() != null) {
			new RefactoringRunner(wc, sel, shellProvider, wc.getCProject()) {
				@Override
				public void run() {
					IntroOpenACCParallelRefactoring refactoring =
							new IntroOpenACCParallelRefactoring(element, sel, project);
					run(new Wizard(refactoring), refactoring, RefactoringSaveHelper.SAVE_NOTHING);
				}
			}.run();
		}
	}

    @Override
	public void updateSelection(ICElement elem) {
    	super.updateSelection(elem);
    	setEnabled(false);
    }

    private static class Wizard extends RefactoringWizard {

    	public Wizard(IntroOpenACCParallelRefactoring refactoring) {
    		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
    		setDefaultPageTitle("Introduce OpenACC Parallel");
    		setDialogSettings(CUIPlugin.getDefault().getDialogSettings());
    	}

    	@Override
    	protected void addUserInputPages() {
    		addPage(new EmptyPage());
    	}
    }

    private static class EmptyPage extends UserInputWizardPage {
    	
    	public EmptyPage() {
			super("(empty)");
		}

    	@Override
    	public void createControl(Composite parent) {
    		Composite c = new Composite(parent, SWT.NONE);
    		c.setLayout(new ColumnLayout());
    		new Button(c, SWT.CHECK).setText("Infer copy clauses");
    		new Button(c, SWT.CHECK).setText("Infer private clause");
    		setControl(c);
    		setTitle(getName());
    		setPageComplete(true);
    	}
    }
}
