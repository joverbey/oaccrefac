package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
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
@SuppressWarnings("restriction")
public class LoopUnrollingRefactoring extends ForLoopRefactoring {

    private int m_unrollFactor;
    
	public LoopUnrollingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
	}

    public void setUnrollFactor(int toSet) {
        if (toSet >= 0) {
            m_unrollFactor = toSet;
        } else {
            throw new IllegalArgumentException("Unroll factor <= 0");
        }
    }
    
    @Override
    protected void doCheckInitialConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        IASTStatement body = getLoop().getBody();
        //if the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement)
            initStatus.addFatalError("Loop body is empty -- nothing to unroll!");
    };

	@Override
	protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {	
        IASTForStatement loop = getLoop();
        IASTCompoundStatement enclosingCompound = 
                ASTUtil.findNearestAncestor(loop, IASTCompoundStatement.class);
        UnrollLoop ul = new UnrollLoop(enclosingCompound, loop, m_unrollFactor);
        rewriter.replace(enclosingCompound, ul.change(), null);
	}

}
