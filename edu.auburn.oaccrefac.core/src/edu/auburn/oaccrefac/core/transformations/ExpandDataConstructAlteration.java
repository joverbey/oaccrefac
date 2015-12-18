package edu.auburn.oaccrefac.core.transformations;

public class ExpandDataConstructAlteration extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

    public ExpandDataConstructAlteration(IASTRewrite rewriter, ExpandDataConstructCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() throws Exception {
        System.out.println("inside the alteration");
    }

}
