package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.InterchangeLoops;

/**
 * This class implements refactoring for loop interchange. Loop interchange is the exchange of the ordering of two
 * iteration variables used in nested loops.
 * 
 */
public class LoopInterchangeRefactoring extends ForLoopRefactoring {

    private int m_depth;

    public LoopInterchangeRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_depth = 1;
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        ForStatementInquisitor forLoop = ForStatementInquisitor.getInquisitor(getLoop());
        if (!forLoop.isPerfectLoopNest()) {
            status.addFatalError("Only perfectly nested loops can be interchanged.");
        }
    }

    public void setExchangeDepth(int depth) {
        if (depth > 0) {
            m_depth = depth;
        }
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        IASTForStatement xchng = ASTUtil.findDepth(getLoop(), IASTForStatement.class, m_depth);
        if (xchng == null) {
            status.addFatalError("Could not find loop at depth " + m_depth);
            return;
        }

        @SuppressWarnings("unused")
        DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(status, pm);
        // if (dependenceAnalysis != null && dependenceAnalysis.()) {
        // status.addError("This loop cannot be parallelized because it carries a dependence.",
        // getLocation(getLoop()));
        // }
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        // Get the top level loop and the loop to refactor.
        IASTForStatement loop = getLoop();
        InterchangeLoops inter = new InterchangeLoops(loop.copy(), m_depth);
        rewriter.replace(getLoop(), inter.change(), null);
    }

}
