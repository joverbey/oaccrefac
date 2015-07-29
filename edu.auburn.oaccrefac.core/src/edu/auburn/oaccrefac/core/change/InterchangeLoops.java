package edu.auburn.oaccrefac.core.change;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.check.InterchangeCheck;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * Inheriting from {@link ForLoopChange}, this class defines a loop interchange
 * refactoring algorithm. Loop interchange swaps the headers of two perfectly
 * nested loops, given that it causes no dependency issues from {@link InterchangeCheck}.
 * 
 * For example,
 *      for (int i = 0; i < 10; i++) {
 *          for (int j = 1; j < 20; j++) {
 *              //do something...
 *          }
 *      }
 * Refactors to:
 *      for (int j = 1; j < 20; j++) {
 *          for (int i = 0; i < 10; i++) {
 *              //do something...
 *          }
 *      }
 * 
 * @author Adam Eichelkraut
 *
 */
public class InterchangeLoops extends ForLoopChange {

    //Members
    private IASTForStatement m_second;

    /**
     * Constructor that takes in two loops to interchange
     * @param rewriter -- rewriter associated with these nodes
     * @param first -- first loop header to exchange, must be outer loop
     * @param second -- second loop, must be apart of perfect loop nest
     * @throws IllegalArgumentException if second loop is null
     */
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
        //Check for perfect loop nest...
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.getLoopToChange());
        if (!inq.isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be interchanged.");
            return init;
        }
        
        //Check to see if the second is within first
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (!headers.contains(m_second)) {
            init.addFatalError("Second loop is not within headers of first");
            
            return init;
        }
        
        //Dependence Analysis...
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
