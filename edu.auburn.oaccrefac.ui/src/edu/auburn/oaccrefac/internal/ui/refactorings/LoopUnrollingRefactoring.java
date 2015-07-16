package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.change.ASTChange;
import edu.auburn.oaccrefac.core.change.IASTRewrite;
import edu.auburn.oaccrefac.core.change.UnrollLoop;

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
public class LoopUnrollingRefactoring extends ForLoopRefactoring {

    private int m_unrollFactor;
    private ASTChange m_unrollChange;
    
	public LoopUnrollingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
	}

    public void setUnrollFactor(int toSet) {
        m_unrollFactor = toSet;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        IASTForStatement loop = getLoop();
        m_unrollChange = new UnrollLoop (null, loop, m_unrollFactor);
        m_unrollChange.checkConditions(initStatus, pm);
    };

	@Override
	protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {	
        m_unrollChange.setRewriter(rewriter);
	    rewriter = m_unrollChange.change();
	    //rewriter.replace(m_unrollChange.getOriginal(), m_unrollChange.change(), null);
	}

}
