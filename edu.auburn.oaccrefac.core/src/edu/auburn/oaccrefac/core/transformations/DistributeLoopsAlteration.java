package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Inheriting from {@link ForLoopAlteration}, this class defines a loop fission refactoring algorithm. Loop fission takes
 * the body of a for-loop and splits the statements into separate for-loops with the same header, if possible.
 * 
 * For example,
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     a[i] = b[i] + c[i];
 *     b[i - 1] = a[i];
 * }
 * </pre>
 * 
 * Refactors to:
 * 
 * <pre>
 * for (int i = 0; i < 10; i++) {
 *     a[i] = b[i] + c[i];
 * }
 * for (int i = 0; i < 10; i++) {
 *     b[i - 1] = a[i];
 * }
 * </pre>
 * 
 * @author Adam Eichelkraut
 */
public class DistributeLoopsAlteration extends ForLoopAlteration<DistributeLoopsCheck> {

    /**
     * Constructor that takes a for-loop to perform fission on
     * 
     * @param rewriter
     *            -- base rewriter for loop
     * @param loop
     *            -- loop to be fizzed
     */
    public DistributeLoopsAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop, DistributeLoopsCheck check) {
        super(tu, rewriter, loop, check);
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
    }

    @Override
    protected void doChange() {
        String init = this.getLoopToChange().getInitializerStatement().getRawSignature();
        String cond = this.getLoopToChange().getConditionExpression().getRawSignature();
        String incr = this.getLoopToChange().getIterationExpression().getRawSignature();

        // Remove the old loop from the statement list
        this.remove(getLoopToChange());

        // Precondition guarantees that body is a compound statement
        IASTCompoundStatement body = (IASTCompoundStatement) this.getLoopToChange().getBody();

        // For each child, create new for loop with same header and child as body
        IASTStatement[] stmts = body.getStatements();
        int offset = this.getLoopToChange().getFileLocation().getNodeOffset();
        for (int i = stmts.length - 1; i >= 0; i--) {
            this.insert(offset, forLoop(init, cond, incr, compound(stmts[i].getRawSignature())));
        }

        finalizeChanges();
    }

}
