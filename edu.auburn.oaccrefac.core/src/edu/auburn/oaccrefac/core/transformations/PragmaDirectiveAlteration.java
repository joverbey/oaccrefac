package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

public abstract class PragmaDirectiveAlteration<T extends PragmaDirectiveCheck<?>> extends SourceAlteration<T> {

    private IASTPreprocessorPragmaStatement pragma;
    private IASTStatement statement;
    
    public PragmaDirectiveAlteration(IASTRewrite rewriter, T check) {
        super(rewriter, check);
        this.pragma = check.getPragma();
        this.statement = check.getStatement();
    }
    
    public IASTPreprocessorPragmaStatement getPragma() {
        return pragma;
    }

    public IASTStatement getStatement() {
        return statement;
    }

}
