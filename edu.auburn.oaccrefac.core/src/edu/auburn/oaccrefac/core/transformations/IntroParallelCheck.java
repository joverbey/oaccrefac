package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;

public class IntroParallelCheck extends ForLoopCheck<RefactoringParameters> {

    public IntroParallelCheck(final IASTForStatement loop) {
        super(loop);
    }

    @Override
    protected void doLoopFormCheck(RefactoringStatus status) {
      //TODO: Check for existing/conflicting OpenACC pragma
    }

    @Override
    public void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        if (dep != null && dep.hasLevel1CarriedDependence()) {
            status.addError("This loop cannot be parallelized because it carries a dependence.");
        }
    }

}
