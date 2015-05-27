package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTForStatement;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Class is meant to be an abstract base class for all ForLoop
 * transformation refactorings. It includes all methods that would
 * be synonomous in all for-loop refactorings. The only method that
 * is different in the refactorings is the 'collectModifications' 
 * method, which is to be overridden in order to make the magic happen.
 *
 */
@SuppressWarnings("restriction")
public abstract class ForLoopRefactoring extends CRefactoring {

    private SubMonitor m_progress;
    private IASTTranslationUnit m_ast;
    private IASTForStatement m_forloop;
    
	public ForLoopRefactoring(ICElement element, ISelection selection, ICProject project) {
		super(element, selection, project);
		//Just pass up to super...
	}

	@Override
	protected abstract void collectModifications(IProgressMonitor pm, ModificationCollector collector)
			throws CoreException, OperationCanceledException;

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		pm.subTask("Waiting for indexer...");
		prepareIndexer(pm);
		pm.subTask("Analyzing selection...");
		
		m_progress = SubMonitor.convert(pm, 10);
        m_ast = getAST(tu, m_progress.newChild(9));
        m_forloop = findLoop(m_ast);
		
		return initStatus;
	}

	private void prepareIndexer(IProgressMonitor pm) throws CoreException  {
		IIndexManager im = CCorePlugin.getIndexManager();
		while (!im.isProjectIndexed(project)) {
			im.joinIndexer(500, pm);
			if (pm.isCanceled())
				throw new CoreException(new Status(IStatus.ERROR, CUIPlugin.PLUGIN_ID, "Cannot continue.  No index."));
		}
		if (!im.isProjectIndexed(project))
			throw new CoreException(new Status(IStatus.ERROR, CUIPlugin.PLUGIN_ID, "Cannot continue.  No index."));
	}	
	
	/**
	 * Since the loop is found via 'findLoop', we are already
	 * assured that the loop is of type CASTForStatement, so
	 * we don't have to check every time.
	 * @return The selected for-loop for refactoring
	 */
	protected CASTForStatement getLoop() {
	    return (CASTForStatement) m_forloop;
	}
	
	protected SubMonitor getProgress() {
	    return m_progress;
	}
	
	protected IASTTranslationUnit getAST() {
	    return m_ast;
	}
	
	/**
	 * This function finds the first CASTForStatement (for-loop) within a
	 * selection. It defines a private class (Visitor) to traverse the
	 * translation unit AST (which is an AST for our source file). When
	 * we call ast.accept(v), it traverses the tree to find the first 
	 * acceptance within the 'selectedRegion' (a protected variable from
	 * CRefactoring). 
	 * @param ast -- our AST to traverse
	 * @return CASTForStatement to perform refactoring on
	 */
	protected CASTForStatement findLoop(IASTTranslationUnit ast) {
		class Visitor extends ASTVisitor {
			private CASTForStatement loop = null;

			public Visitor() {
				shouldVisitStatements = true;
			}

			@Override
			public int visit(IASTStatement statement) {
				if (statement instanceof CASTForStatement) {
					CASTForStatement for_stmt = (CASTForStatement) statement;
					//Make sure we are getting the correct loop by checking the statement's 
					//offset with the selected text's region in the project.
					int begin_offset = selectedRegion.getOffset();
					int end_offset = selectedRegion.getOffset() + selectedRegion.getLength();
					if (for_stmt.getOffset() >= begin_offset
						&& (for_stmt.getOffset() < end_offset)) {
						loop = for_stmt;
						return PROCESS_ABORT;
					} else {
						//Otherwise skip this statement
						return PROCESS_CONTINUE;
					}
				}
				return PROCESS_CONTINUE;
			}
		}
		Visitor v = new Visitor();
		ast.accept(v);
		return v.loop;
	}
}
