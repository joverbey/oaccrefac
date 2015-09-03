package edu.auburn.oaccrefac.core.transformations;

public class IntroParallelAlteration extends ForLoopAlteration<IntroParallelCheck> {

    public IntroParallelAlteration(IASTRewrite rewriter, IntroParallelCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() {
        int offset = getLoopToChange().getFileLocation().getNodeOffset();
        this.insert(offset, pragma("acc parallel loop"));
        finalizeChanges();
    }

}
