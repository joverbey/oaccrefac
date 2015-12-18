package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

public class ExpandDataConstructCheck extends PragmaDirectiveCheck<RefactoringParams> {

    public ExpandDataConstructCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        super(pragma, statement);
    }

}
