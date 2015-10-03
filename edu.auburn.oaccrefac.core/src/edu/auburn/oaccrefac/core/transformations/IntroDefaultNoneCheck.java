package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;

public class IntroDefaultNoneCheck extends PragmaDirectiveCheck<RefactoringParams> {

    public IntroDefaultNoneCheck(IASTPreprocessorPragmaStatement pragma) {
        super(pragma);
    }
    
}
