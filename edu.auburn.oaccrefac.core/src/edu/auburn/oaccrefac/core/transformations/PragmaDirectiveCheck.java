package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

public class PragmaDirectiveCheck<T extends RefactoringParams> extends Check<T> {

    private IASTPreprocessorPragmaStatement pragma;

    public PragmaDirectiveCheck(IASTPreprocessorPragmaStatement pragma) {
        this.pragma = pragma;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return pragma.getTranslationUnit();
    }
    
    public IASTPreprocessorPragmaStatement getPragma() {
        return pragma;
    }

}
