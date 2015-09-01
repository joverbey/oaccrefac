package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;

public class IntroParallelCheck extends Check {

    public IntroParallelCheck(final IASTForStatement loop) {
        super(loop);
    }

    @Override
    public void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        // TODO: Check for existing/conflicting OpenACC pragma

        if (dep != null && dep.hasLevel1CarriedDependence()) {
            status.addError("This loop cannot be parallelized because it carries a dependence.");
        }
    }

}
