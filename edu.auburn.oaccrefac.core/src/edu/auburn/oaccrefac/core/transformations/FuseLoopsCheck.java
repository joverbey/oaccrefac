package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

public class FuseLoopsCheck extends ForLoopCheck<RefactoringParams> {

    private IASTForStatement first;
    private IASTForStatement second;

    public FuseLoopsCheck(IASTForStatement first) {
        super(first);
        this.first = this.loop;
        
        // This gets the selected loop to re-factor.
        IASTNode next = ASTUtil.getNextSibling(first);

        // Create pattern for matching loop headers
        IASTForStatement pattern = first.copy();
        pattern.setBody(new ArbitraryStatement());

        if (next != null && next instanceof IASTForStatement) {

            // Check to make sure the first and second loops have same headers
            if (ASTMatcher.unify(pattern, next) != null) {
                second = (IASTForStatement) next;
            } else {
                second = null;
            }
        }
        else {
            second = null;
        }
    }

    public IASTForStatement getLoop1() {
        return this.first;
    }
    
    public IASTForStatement getLoop2() {
        return this.second;
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
