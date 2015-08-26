package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class DistributeLoopsCheck extends DependenceCheck {
    private IASTForStatement loop;

    public DistributeLoopsCheck(final IASTForStatement loop) {
        super(loop);
        this.loop = loop;
    }

    @Override
    public RefactoringStatus doCheck(RefactoringStatus status) {
        // If the loop doesn't have children, bail.
        if (!(loop.getBody() instanceof IASTCompoundStatement)) {
            status.addFatalError("Body does not have any statements, so loop fission is useless.");
            return status;
        }

        if (loop.getBody().getChildren().length < 2) {
            status.addFatalError("Loop fission refactoring requires more than one statement.");
            return status;
        }

        return status;
    }

}
