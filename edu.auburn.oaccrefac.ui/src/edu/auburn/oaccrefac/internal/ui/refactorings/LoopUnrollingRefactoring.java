package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.UnrollLoopAlteration;
import edu.auburn.oaccrefac.core.transformations.UnrollLoopCheck;
import edu.auburn.oaccrefac.core.transformations.UnrollLoopParams;
import edu.auburn.oaccrefac.internal.core.Activator;

/**
 * This class defines the implementation for refactoring a loop so that it is unrolled. For example:
 * 
 * <pre>
 * ORIGINAL:				REFACTORED:
 * int x;					|  int x;
 * for (x=0; x<100; x++)	|  for (x=0; x<100; x++) {
 *   delete(a[x]);			|    delete(a[x]); x++;
 *   						|    delete(a[x]); x++;
 *   						|	 delete(a[x]); x++;
 *   						|	 delete(a[x]); x++;
 *  						|	 delete(a[x]);
 *  						|  }
 * </pre>
 * 
 * (Example taken from Wikipedia's webpage on loop unrolling)
 */
public class LoopUnrollingRefactoring extends ForLoopRefactoring {

    private int unrollFactor;
    private UnrollLoopCheck check;

    public LoopUnrollingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    public void setUnrollFactor(int toSet) {
        unrollFactor = toSet;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new UnrollLoopCheck(getLoop());
        check.performChecks(status, pm, new UnrollLoopParams(unrollFactor));
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        try {
            UnrollLoopAlteration unroll = new UnrollLoopAlteration(rewriter, unrollFactor, check);
            unroll.change();
        } catch (DOMException e) {
            Activator.log(e);
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
    }

}
