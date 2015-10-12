package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

public class PragmaDirectiveCheck<T extends RefactoringParams> extends Check<T> {

    private IASTPreprocessorPragmaStatement pragma;
    private IASTStatement statement;

    public PragmaDirectiveCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        this.pragma = pragma;
        this.statement = statement;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return pragma.getTranslationUnit();
    }
    
    public IASTPreprocessorPragmaStatement getPragma() {
        return pragma;
    }

    public IASTStatement getStatement() {
        return statement;
    }
    
}
