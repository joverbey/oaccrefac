package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTStatement;

public class IntroduceDataConstructAlteration extends SourceStatementsAlteration<IntroduceDataConstructCheck> {

    public IntroduceDataConstructAlteration(IASTRewrite rewriter, IntroduceDataConstructCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() throws Exception {
        IASTStatement[] stmts = getStatements();
        String origRegion = "";
        for(IASTStatement stmt : stmts) {
            origRegion += stmt.getRawSignature();
        }
        String replacement = pragma("acc data") + compound(decompound(origRegion));
        this.replace(getOffset(), getLength(), replacement);
        finalizeChanges();
    }

}
