package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;

public abstract class PragmaDirectiveAlteration<T extends PragmaDirectiveCheck<?>> extends SourceAlteration<T> {

    private IASTPreprocessorPragmaStatement pragma;
    
    public PragmaDirectiveAlteration(IASTRewrite rewriter, T check) {
        super(rewriter, check);
        this.pragma = check.getPragma();
    }
    
    public IASTPreprocessorPragmaStatement getPragma() {
        return pragma;
    }

}
