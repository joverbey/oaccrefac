package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

public class IntroParallelAlteration extends ForLoopAlteration<IntroParallelCheck> {

    public IntroParallelAlteration(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop, IntroParallelCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() {
        int offset = getLoopToChange().getFileLocation().getNodeOffset();
        this.insert(offset, pragma("acc parallel loop"));
        finalizeChanges();
    }

}
