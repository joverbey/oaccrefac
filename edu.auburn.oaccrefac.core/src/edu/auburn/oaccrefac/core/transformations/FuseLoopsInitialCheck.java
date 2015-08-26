package edu.auburn.oaccrefac.core.transformations;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

public class FuseLoopsInitialCheck extends Check {

    private IASTForStatement first;
    private IASTForStatement second;

    public FuseLoopsInitialCheck(IASTForStatement first) {
        this.first = first;
    }

    @Override
    public RefactoringStatus doCheck(RefactoringStatus status) {
        // This gets the selected loop to re-factor.
        boolean found = false;
        IASTNode newnode = first;

        // Create pattern for matching loop headers
        IASTForStatement pattern = first.copy();
        pattern.setBody(new ArbitraryStatement());

        if (ASTUtil.getNextSibling(newnode) != null) {
            newnode = ASTUtil.getNextSibling(newnode);
            second = ASTUtil.findOne(newnode, IASTForStatement.class);
            found = (second != null);

            // Check to make sure the first and second loops have same headers
            Map<String, String> varmap = ASTMatcher.unify(pattern, second);
            if (varmap != null) {
                for (String key : varmap.keySet()) {
                    // The map returned contains name mapping that
                    // tells which names would make the two patterns equal
                    if (!varmap.get(key).equals(key)) {
                        found = false;
                    }
                }
                found = true;
            } else {
                found = false;
            }
        }

        if (!found) {
            status.addFatalError("There is no for loop for fusion to be possible.");
            return status;
        }

        return status;
    }

}
