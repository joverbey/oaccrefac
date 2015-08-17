package edu.auburn.oaccrefac.core.dependence.check;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;

public class FusionInitialCheck extends DependenceCheck {

    public FusionInitialCheck(IASTForStatement first, IASTForStatement second) {
        super(first);
        
    }

    @Override
    public RefactoringStatus doCheck(RefactoringStatus status, DependenceAnalysis dep) {
        // TODO Auto-generated method stub
        return null;
    }

}
