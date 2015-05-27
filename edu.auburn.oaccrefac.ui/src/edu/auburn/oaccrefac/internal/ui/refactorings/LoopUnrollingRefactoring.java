package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTForStatement;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

/**
 * This class defines the implementation for refactoring a loop
 * so that it is unrolled. For example:
 * 
 * ORIGINAL:				REFACTORED:
 * int x;					|  int x;
 * for (x=0; x<100; x++)	|  for (x=0; x<100; x++) {
 *   delete(a[x]);			|    delete(a[x]); x++;
 *   						|    delete(a[x]); x++;
 *   						|	 delete(a[x]); x++;
 *   						|	 delete(a[x]); x++;
 *  						|	 delete(a[x]);
 *  						|  }
 * (Example taken from Wikipedia's webpage on loop unrolling)
 */
@SuppressWarnings("restriction")
public class LoopUnrollingRefactoring extends ForLoopRefactoring {

    private int m_unrollFactor;
    
	public LoopUnrollingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        if (selection == null || tu.getResource() == null || project == null)
            initStatus.addFatalError("Invalid selection");
	}

    public void setUnrollFactor(int toSet) {
        if (toSet >= 0) {
            m_unrollFactor = toSet;
        } else {
            throw new IllegalArgumentException("Unroll factor <= 0");
        }
    }

	@Override
	protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
			throws CoreException, OperationCanceledException {
	    
	    ASTRewrite rewriter = collector.rewriterForTranslationUnit(getAST());
	    CASTForStatement loop = getLoop();
	    
	    
//	    getLoopUpperBound(loop);
	      
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

}
