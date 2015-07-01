package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.Change;
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.UnrollLoop;

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
    private Change<?> m_unrollChange;
    
	public LoopUnrollingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
	}

    public void setUnrollFactor(int toSet) {
        m_unrollFactor = toSet;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        IASTForStatement loop = getLoop();
        IASTCompoundStatement enclosingCompound = 
                ASTUtil.findNearestAncestor(loop, IASTCompoundStatement.class);
        m_unrollChange = new UnrollLoop(enclosingCompound, loop, m_unrollFactor);
        m_unrollChange.setProgressMonitor(pm);
        m_unrollChange.checkConditions(initStatus);
    };

	@Override
	protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {	
        rewriter = m_unrollChange.change(rewriter);
	    //rewriter.replace(m_unrollChange.getOriginal(), m_unrollChange.change(), null);
	}

}
