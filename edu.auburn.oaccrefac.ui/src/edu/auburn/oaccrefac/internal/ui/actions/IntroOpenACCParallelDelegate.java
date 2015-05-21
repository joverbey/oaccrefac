package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.forms.widgets.ColumnLayout;

import edu.auburn.oaccrefac.internal.ui.refactorings.IntroOpenACCParallelRefactoring;

/**
 * Our action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
@SuppressWarnings("restriction")
public class IntroOpenACCParallelDelegate extends RefactoringActionDelegate {
	
	/**
	 * The constructor.
	 */
	public IntroOpenACCParallelDelegate() {
		super();
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		//TODO: make this better...? maybe?
		if (selection instanceof ITextSelection)
			setSelection((ITextSelection) selection);
	}
	
	@Override
	public CRefactoring createRefactoring(IWorkingCopy wc, ITextSelection selection, ICProject project) {
		return new IntroOpenACCParallelRefactoring(wc, selection, project);
	}

	@Override
	public RefactoringWizard createWizard(Refactoring refactoring) {
		return new Wizard(refactoring);
	}
	
    private static class Wizard extends RefactoringWizard {

    	public Wizard(Refactoring refactoring) {
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