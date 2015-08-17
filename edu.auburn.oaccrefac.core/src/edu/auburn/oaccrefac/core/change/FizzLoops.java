package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Inheriting from {@link ForLoopChange}, this class defines a loop fission refactoring algorithm. Loop fission takes
 * the body of a for-loop and splits the statements into separate for-loops with the same header, if possible.
 * 
 * For example, for (int i = 0; i < 10; i++) { a[i] = b[i] + c[i]; b[i-1] = a[i]; } Refactors to: for (int i = 0; i <
 * 10; i++) { a[i] = b[i] + c[i]; } for (int i = 0; i < 10; i++) { b[i-1] = a[i]; }
 * 
 * @author Adam Eichelkraut
 *
 */
public class FizzLoops extends ForLoopChange {

    /**
     * Constructor that takes a for-loop to perform fission on
     * 
     * @param rewriter
     *            -- base rewriter for loop
     * @param loop
     *            -- loop to be fizzed
     */
    public FizzLoops(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop) {
        super(tu, rewriter, loop);
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
    }

    @Override
    protected void doChange() {
        // ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        //
        // //Ensured from precondition...
        // IASTForStatement loop = this.getLoopToChange();
        // IASTCompoundStatement body = (IASTCompoundStatement) loop.getBody();
        //
        // //Get the location to insert the separate loops
        // IASTNode insertBefore = ASTUtil.getNextSibling(loop);
        //
        // //For each child, create new for loop with same header and child as body
        // for (IASTStatement child : body.getStatements()) {
        // IASTCompoundStatement newBody = factory.newCompoundStatement();
        // newBody.addStatement(child.copy());
        // IASTForStatement newForLoop = factory.newForStatement
        // (loop.getInitializerStatement().copy(),
        // loop.getConditionExpression().copy(),
        // loop.getIterationExpression().copy(),
        // newBody);
        // //place before the insertion point
        // this.safeInsertBefore(rewriter,
        // loop.getParent(), insertBefore, newForLoop);
        // }
        //
        // //Remove the old loop from the statement list
        // if (body.getStatements().length > 0) {
        // this.safeRemove(rewriter, loop);
        // }
        //
        // return rewriter;
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
