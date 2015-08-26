package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.SourceAlteration;
import edu.auburn.oaccrefac.core.transformations.DependenceCheck;
import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.InterchangeLoopsCheck;
import edu.auburn.oaccrefac.core.transformations.InterchangeLoopsAlteration;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

/**
 * This class implements refactoring for loop interchange. Loop interchange is the exchange of the ordering of two
 * iteration variables used in nested loops.
 * 
 */
public class LoopInterchangeRefactoring extends ForLoopRefactoring {

    private int m_depth;
    private IASTForStatement m_first;
    private IASTForStatement m_second;

    public LoopInterchangeRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_depth = 1;
    }

    public void setExchangeDepth(int depth) {
        m_depth = depth;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.getLoop());
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (m_depth < 0 || m_depth >= headers.size()) {
            status.addFatalError("There is no for-loop at exchange depth:" + m_depth);
        }
        
        m_first = this.getLoop();
        m_second = ASTUtil.findDepth(m_first, IASTForStatement.class, m_depth);

        DependenceCheck check = new InterchangeLoopsCheck(m_first, m_second);
        check.check(status, pm);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {
        SourceAlteration change = new InterchangeLoopsAlteration(getAST(), rewriter, m_first, m_second);
        change.change();
    }

}
