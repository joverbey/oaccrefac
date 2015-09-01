package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class IntroParallelAlteration extends ForLoopAlteration<IntroParallelCheck> {

    public IntroParallelAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop, IntroParallelCheck check) {
        super(tu, rewriter, loop, check);
    }

    @Override
    protected void doChange() {
        int offset = getLoopToChange().getFileLocation().getNodeOffset();
        this.insert(offset, pragma("acc parallel loop"));
        finalizeChanges();
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        // TODO Auto-generated method stub

    }

}
