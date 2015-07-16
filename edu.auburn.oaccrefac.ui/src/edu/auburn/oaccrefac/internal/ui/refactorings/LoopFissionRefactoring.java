package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.change.ASTChange;
import edu.auburn.oaccrefac.core.change.FizzLoops;
import edu.auburn.oaccrefac.core.change.IASTRewrite;

/**
 * This class defines the implementation for re-factoring using loop fusion. For example:
 * 
 * ORIGINAL:                  REFACTORED: 
 * int i, a[100], b[100];     | int i, a[100], b[100]; 
 * for (i = 0; i < 100; i++) {| for (i = 0; i <100; i++)
 *    a[i] = 1;               |     a[i] = 1;
 *    b[i] = 2;               | for (i = 0; i <100; i++)
 * }                          |     b[i] = 2; 
 *                            |
 * 
 * (Example taken from Wikipedia's web page on loop Fission.)
 */
public class LoopFissionRefactoring extends ForLoopRefactoring {

    //I've always wanted to use a question mark like this.
    private ASTChange m_fizzChange;
    
    public LoopFissionRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        m_fizzChange = new FizzLoops(null, getLoop());
        m_fizzChange.checkConditions(initStatus, pm);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {
        m_fizzChange.setRewriter(rewriter);
        rewriter = m_fizzChange.change();
        //rewriter.replace(m_fizzChange.getOriginal(), m_fizzChange.change(), null);
    }

}
