package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

public class IntroDefaultNoneCheck extends PragmaDirectiveCheck<RefactoringParams> {

    public IntroDefaultNoneCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        super(pragma, statement);
    }
    
}
