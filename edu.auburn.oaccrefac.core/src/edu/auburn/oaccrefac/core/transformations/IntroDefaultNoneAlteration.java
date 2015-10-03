package edu.auburn.oaccrefac.core.transformations;

public class IntroDefaultNoneAlteration extends PragmaDirectiveAlteration<IntroDefaultNoneCheck> {

    public IntroDefaultNoneAlteration(IASTRewrite rewriter, IntroDefaultNoneCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() throws Exception {
//        this.insert(getPragma().getFileLocation().getNodeOffset() + getPragma().getFileLocation().getNodeLength(), "//test");
        finalizeChanges();
    }

}
