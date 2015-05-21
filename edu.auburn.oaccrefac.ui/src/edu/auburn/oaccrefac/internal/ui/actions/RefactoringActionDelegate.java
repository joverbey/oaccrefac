package edu.auburn.oaccrefac.internal.ui.actions;

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
	private IWorkbenchWindow m_window;
	private ITextSelection m_selection;
	
	public RefactoringActionDelegate() {}
	
	@Override
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
				new RefactoringRunner((ICElement)wc, m_selection, 
						editor.getSite(), wc.getCProject()) {
					@Override
					public void run() {
						CRefactoring refac = createRefactoring(wc, m_selection, project);
						run(createWizard(refac), refac, RefactoringSaveHelper.SAVE_NOTHING);
					}
				}.run();
			}
		}
	}

	@Override
	public abstract void selectionChanged(IAction action, ISelection selection);
	
	public abstract CRefactoring createRefactoring(IWorkingCopy wc,
												ITextSelection selection,
												ICProject project);
	public abstract RefactoringWizard createWizard(Refactoring refactoring);
												
	
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
	
	public ITextSelection getSelection() {return m_selection;}
	
	public void setSelection(ITextSelection toSet) {
		m_selection = toSet;
	}

}
