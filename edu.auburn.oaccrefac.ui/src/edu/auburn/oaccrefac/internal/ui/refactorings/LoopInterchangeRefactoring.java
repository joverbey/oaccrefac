package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.ui.refactorings.changes.InterchangeLoops;

/**
 * This class implements refactoring for loop interchange. Loop interchange is the exchange of the ordering of two
 * iteration variables used in nested loops.
 * 
 */
public class LoopInterchangeRefactoring extends ForLoopRefactoring {

    private int m_depth;
    private InterchangeLoops change;

    public LoopInterchangeRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_depth = 1;
    }

    public void setExchangeDepth(int depth) {
        m_depth = depth;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        change = new InterchangeLoops(getLoop(), m_depth);
        change.setProgressMonitor(pm);
        change.checkConditions(status);
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        rewriter.replace(getLoop(), change.change(), null);
    }

}
