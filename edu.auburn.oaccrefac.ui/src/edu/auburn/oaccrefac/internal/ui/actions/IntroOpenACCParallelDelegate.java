package edu.auburn.oaccrefac.internal.ui.actions;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.internal.ui.editor.CEditor;
import org.eclipse.cdt.internal.ui.refactoring.RefactoringRunner;
import org.eclipse.cdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ColumnLayout;

import edu.auburn.oaccrefac.internal.ui.refactorings.IntroOpenACCParallelRefactoring;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
@SuppressWarnings("restriction")
public class IntroOpenACCParallelDelegate implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow m_window;
	private ISelection m_selection;
	/**
	 * The constructor.
	 */
	public IntroOpenACCParallelDelegate() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		//Get the CEditor in order to get the project
		CEditor editor = (CEditor) PlatformUI.getWorkbench()
				  .getActiveWorkbenchWindow()
				  .getActivePage()
				  .getActiveEditor();
		
		if (editor != null) {
			final IWorkingCopy wc = CUIPlugin.getDefault()
					.getWorkingCopyManager()
					.getWorkingCopy(editor.getEditorInput());
					
			if (wc != null) {
				new RefactoringRunner((ICElement)wc, m_selection, editor.getSite(), wc.getCProject()) {
					@Override
					public void run() {
						IntroOpenACCParallelRefactoring refactoring = 
								new IntroOpenACCParallelRefactoring(wc, (ITextSelection)m_selection, project);
						run(new Wizard(refactoring), refactoring, RefactoringSaveHelper.SAVE_NOTHING);
					}
				}.run();
			}
		}
	}
	
	

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		m_selection = selection; //TODO: Make this better. For now, just trust
								//the user does the right thing...
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.m_window = window;
	}
	
	public IWorkbenchWindow getWindow() {return m_window;}
	public void setWindow(IWorkbenchWindow w) {
		m_window = w;
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