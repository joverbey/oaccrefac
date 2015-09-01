package edu.auburn.oaccrefac.core.transformations;

import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop interchange refactoring algorithm. Loop interchange
 * swaps the headers of two perfectly nested loops, given that it causes no dependency issues from
 * {@link InterchangeLoopsCheck}.
 * 
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     for (int j = 1; j < 20; j++) {
 *         // do something...
 *     }
 * }
 * </pre>
 * 
 * Refactors to:
 * 
 * <pre>
 * for (int j = 1; j < 20; j++) {
 *     for (int i = 0; i < 10; i++) {
 *         // do something...
 *     }
 * }
 * </pre>
 * 
 * @author Adam Eichelkraut
 */
public class InterchangeLoopsAlteration extends ForLoopAlteration<InterchangeLoopsCheck> {

    private IASTForStatement second;

    /**
     * Constructor that takes in two loops to interchange
     * 
     * @param rewriter
     *            -- rewriter associated with these nodes
     * @param first
     *            -- first loop header to exchange, must be outer loop
     * @param second
     *            -- second loop, must be apart of perfect loop nest
     * @throws IllegalArgumentException
     *             if second loop is null
     */
    public InterchangeLoopsAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement first,
            IASTForStatement second, InterchangeLoopsCheck check) {
        super(tu, rewriter, first, check);
        if (second != null) {
            this.second = second;
        } else {
            throw new IllegalArgumentException("Target loop cannot be null!");
        }
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        // Check for perfect loop nest...
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(this.getLoopToChange());
        if (!inq.isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be interchanged.");
            return;
        }

        // Check to see if the second is within first
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (!headers.contains(second)) {
            init.addFatalError("Second loop is not within headers of first");

            return;
        }

        // Dependence Analysis...
        // InterchangeCheck checkDependence = new InterchangeCheck(getLoopToChange(), m_second);
        // init = checkDependence.check(init, this.getProgressMonitor());

        return;
    }

    @Override
    protected void doChange() {
        IASTForStatement first = getLoopToChange();
        List<IASTPreprocessorPragmaStatement> firstPrags = getPragmas(first);
        List<IASTPreprocessorPragmaStatement> secondPrags = getPragmas(second);
        Collections.reverse(firstPrags);
        Collections.reverse(secondPrags);

        replace(second.getIterationExpression(), first.getIterationExpression().getRawSignature());
        replace(second.getConditionExpression(), first.getConditionExpression().getRawSignature());
        replace(second.getInitializerStatement(), first.getInitializerStatement().getRawSignature());
        for (IASTPreprocessorPragmaStatement prag : firstPrags) {
            insert(second.getFileLocation().getNodeOffset(), prag.getRawSignature() + System.lineSeparator());
        }
        for (IASTPreprocessorPragmaStatement prag : secondPrags) {
            remove(prag.getFileLocation().getNodeOffset(),
                    prag.getFileLocation().getNodeLength() + System.lineSeparator().length());
        }

        replace(first.getIterationExpression(), second.getIterationExpression().getRawSignature());
        replace(first.getConditionExpression(), second.getConditionExpression().getRawSignature());
        replace(first.getInitializerStatement(), second.getInitializerStatement().getRawSignature());
        for (IASTPreprocessorPragmaStatement prag : secondPrags) {
            insert(first.getFileLocation().getNodeOffset(), prag.getRawSignature() + System.lineSeparator());
        }
        for (IASTPreprocessorPragmaStatement prag : firstPrags) {
            remove(prag.getFileLocation().getNodeOffset(),
                    prag.getFileLocation().getNodeLength() + System.lineSeparator().length());
        }

        finalizeChanges();
    }
}
