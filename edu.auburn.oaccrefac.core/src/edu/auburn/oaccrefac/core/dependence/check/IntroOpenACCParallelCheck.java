package edu.auburn.oaccrefac.core.dependence.check;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;

public class IntroOpenACCParallelCheck extends DependenceCheck {

    
    public IntroOpenACCParallelCheck(final IASTForStatement loop) {
        super(loop);
    }
    
    @Override
    public RefactoringStatus doCheck(RefactoringStatus status) {
        // TODO: Check for existing/conflicting OpenACC pragma

        DependenceAnalysis dep = getDependenceAnalysis();
        
        if (dep != null && dep.hasLevel1CarriedDependence()) {
            status.addError("This loop cannot be parallelized because it carries a dependence.");
        }
        return status;
    }

}
