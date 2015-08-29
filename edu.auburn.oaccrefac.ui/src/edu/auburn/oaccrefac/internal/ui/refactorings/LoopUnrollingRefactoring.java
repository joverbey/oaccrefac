package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.SourceAlteration;
import edu.auburn.oaccrefac.core.transformations.UnrollLoopAlteration;
import edu.auburn.oaccrefac.core.transformations.IASTRewrite;

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

    private int unrollFactor;
    private SourceAlteration unroll;
    
	public LoopUnrollingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
	}

    public void setUnrollFactor(int toSet) {
        unrollFactor = toSet;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        IASTForStatement loop = getLoop();
    }

	@Override
	protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {
	    SourceAlteration unroll = new UnrollLoopAlteration(getAST(), rewriter, getLoop(), unrollFactor);
	    unroll.change();
	}

}
