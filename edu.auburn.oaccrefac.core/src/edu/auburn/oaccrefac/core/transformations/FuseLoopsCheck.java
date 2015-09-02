package edu.auburn.oaccrefac.core.transformations;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

public class FuseLoopsCheck extends Check<RefactoringParameters> {

    private IASTForStatement first;
    private IASTForStatement second;

    public FuseLoopsCheck(IASTForStatement first) {
        super(first);
        this.first = this.loop;
        
        // This gets the selected loop to re-factor.
        // TODO does the commented out code do anything?
        // boolean found = false;
        IASTNode next = ASTUtil.getNextSibling(first);

        // Create pattern for matching loop headers
        IASTForStatement pattern = first.copy();
        pattern.setBody(new ArbitraryStatement());

        if (next != null && next instanceof IASTForStatement) {
//            found = (second != null);

            // Check to make sure the first and second loops have same headers
            Map<String, String> varmap = ASTMatcher.unify(pattern, next);
            if (varmap != null) {
//                for (String key : varmap.keySet()) {
//                    // The map returned contains name mapping that
//                    // tells which names would make the two patterns equal
//                    if (!varmap.get(key).equals(key)) {
//                        found = false;
//                    }
//                }
//                found = true;
                second = (IASTForStatement) next;
            } else {
//                found = false;
                second = null;
            }
        }
        else {
            second = null;
        }
    }

    @Override
    public void doLoopFormCheck(RefactoringStatus status) {
        if (second == null) {
            status.addFatalError("There is no for loop for fusion to be possible.");
        }
    }

  
    @Override
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        //TODO figure out how to do this dependence analysis
    }
    
}
