package edu.auburn.oaccrefac.core.transformations;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

public class FuseLoopsCheck extends Check {

    private IASTForStatement first;
    private IASTForStatement second;

    public FuseLoopsCheck(IASTForStatement first) {
        super(first);
        this.first = first;
        
        // This gets the selected loop to re-factor.
        // TODO does the commented out code do anything?
        // boolean found = false;
        IASTNode newnode = first;

        // Create pattern for matching loop headers
        IASTForStatement pattern = first.copy();
        pattern.setBody(new ArbitraryStatement());

        if (ASTUtil.getNextSibling(newnode) != null) {
            newnode = ASTUtil.getNextSibling(newnode);
            IASTForStatement next = ASTUtil.findOne(newnode, IASTForStatement.class);
//            found = (second != null);

            // Check to make sure the first and second loops have same headers
            Map<String, String> varmap = ASTMatcher.unify(pattern, second);
            if (varmap != null) {
//                for (String key : varmap.keySet()) {
//                    // The map returned contains name mapping that
//                    // tells which names would make the two patterns equal
//                    if (!varmap.get(key).equals(key)) {
//                        found = false;
//                    }
//                }
//                found = true;
                second = next;
            } else {
//                found = false;
                second = null;
            }
        }
    }

    @Override
    public void doLoopFormCheck(RefactoringStatus status) {
        if (second == null) {
            status.addFatalError("There is no for loop for fusion to be possible.");
        }
    }

}
