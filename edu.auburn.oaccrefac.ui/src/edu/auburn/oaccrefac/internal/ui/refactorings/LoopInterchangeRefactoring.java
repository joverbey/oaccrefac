package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.InterchangeLoopsAlteration;
import edu.auburn.oaccrefac.core.transformations.InterchangeLoopsCheck;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

/**
 * This class implements refactoring for loop interchange. Loop interchange is the exchange of the ordering of two
 * iteration variables used in nested loops.
 * 
 */
public class LoopInterchangeRefactoring extends ForLoopRefactoring {

    private int depth;
    private IASTForStatement first;
    private IASTForStatement second;
    private InterchangeLoopsCheck check;

    public LoopInterchangeRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        depth = 1;
    }

    public void setExchangeDepth(int depth) {
        this.depth = depth;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.getLoop());
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (depth < 0 || depth >= headers.size()) {
            status.addFatalError("There is no for-loop at exchange depth:" + depth);
        }
        
        first = this.getLoop();
        second = ASTUtil.findDepth(first, IASTForStatement.class, depth);

        check = new InterchangeLoopsCheck(first, second);
        check.performChecks(status, pm, null);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {
        new InterchangeLoopsAlteration(getAST(), rewriter, first, second, check).change();
    }

}
