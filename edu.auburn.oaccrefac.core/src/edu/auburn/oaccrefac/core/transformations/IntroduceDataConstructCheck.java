package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTStatement;

public class IntroduceDataConstructCheck extends SourceStatementsCheck<RefactoringParams> {

    public IntroduceDataConstructCheck(IASTStatement[] statements) {
        super(statements);
    }


}
