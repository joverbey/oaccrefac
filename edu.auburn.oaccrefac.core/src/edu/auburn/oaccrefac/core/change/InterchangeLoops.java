package edu.auburn.oaccrefac.core.change;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.check.InterchangeCheck;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class InterchangeLoops extends ForLoopChange {

    private IASTForStatement m_second;

    public InterchangeLoops(IASTRewrite rewriter,
            IASTForStatement first, IASTForStatement second) {
        super(rewriter, first);
        if (second != null) {
            m_second = second;
        } else {
            throw new IllegalArgumentException("Target loop cannot be null!");
        }
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.getLoopToChange());
        if (!inq.isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be interchanged.");
            return init;
        }
        
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (!headers.contains(m_second)) {
            init.addFatalError("Second loop is not within headers of first");
            
            return init;
        }
        
        InterchangeCheck checkDependence = new InterchangeCheck(getLoopToChange(), m_second);
        init = checkDependence.check(init, this.getProgressMonitor());
        
        return init;
    }
    
    @Override
    public IASTRewrite doChange(IASTRewrite rewriter) {
        IASTForStatement first = getLoopToChange();
        return this.exchangeLoopHeaders(rewriter, first, m_second);
    }

    
    
}
