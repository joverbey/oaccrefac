package edu.auburn.oaccrefac.core.change;

import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
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
    public InterchangeLoops(IASTTranslationUnit tu, IASTRewrite rewriter,
            IASTForStatement first, IASTForStatement second) {
        super(tu, rewriter, first);
        if (second != null) {
            m_second = second;
        } else {
            throw new IllegalArgumentException("Target loop cannot be null!");
        }
    }
    
    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        //Check for perfect loop nest...
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.getLoopToChange());
        if (!inq.isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be interchanged.");
            return;
        }
        
        //Check to see if the second is within first
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (!headers.contains(m_second)) {
            init.addFatalError("Second loop is not within headers of first");
            
            return;
        }
        
        //Dependence Analysis...
//        InterchangeCheck checkDependence = new InterchangeCheck(getLoopToChange(), m_second);
//        init = checkDependence.check(init, this.getProgressMonitor());
        
        return;
    }
    
    @Override
    protected void doChange() {
        IASTForStatement first = getLoopToChange();
        List<IASTPreprocessorPragmaStatement> firstPrags = getPragmas(first);
        List<IASTPreprocessorPragmaStatement> secondPrags = getPragmas(m_second);
        Collections.reverse(firstPrags);
        Collections.reverse(secondPrags);

        replace(m_second.getIterationExpression(), first.getIterationExpression().getRawSignature());
        replace(m_second.getConditionExpression(), first.getConditionExpression().getRawSignature());
        replace(m_second.getInitializerStatement(), first.getInitializerStatement().getRawSignature());
        for(IASTPreprocessorPragmaStatement prag : firstPrags) {
            insert(m_second.getFileLocation().getNodeOffset(), prag.getRawSignature() + System.lineSeparator());
        }
        for(IASTPreprocessorPragmaStatement prag : secondPrags) {
            remove(prag.getFileLocation().getNodeOffset(), prag.getFileLocation().getNodeLength() + System.lineSeparator().length());
        }
        
        replace(first.getIterationExpression(), m_second.getIterationExpression().getRawSignature());
        replace(first.getConditionExpression(), m_second.getConditionExpression().getRawSignature());
        replace(first.getInitializerStatement(), m_second.getInitializerStatement().getRawSignature());
        for(IASTPreprocessorPragmaStatement prag : secondPrags) {
            insert(first.getFileLocation().getNodeOffset(), prag.getRawSignature() + System.lineSeparator());
        }
        for(IASTPreprocessorPragmaStatement prag : firstPrags) {
            remove(prag.getFileLocation().getNodeOffset(), prag.getFileLocation().getNodeLength() + System.lineSeparator().length());
        }
        
        finalizeChanges();
    }
    
    
}
