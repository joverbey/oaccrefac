package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class IntroOpenACCParallelChange extends ForLoopChange {

    public IntroOpenACCParallelChange(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loopToChange) {
        super(tu, rewriter, loopToChange);
    }

    @Override
    protected void doChange() {
        int offset = getLoopToChange().getFileLocation().getNodeOffset();
        this.insert(offset, pragma("openacc parallel"));
        finalizeChanges();
    }

    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        // TODO Auto-generated method stub

    }

}
