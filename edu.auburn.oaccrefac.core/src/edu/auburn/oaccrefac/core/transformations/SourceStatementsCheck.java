package edu.auburn.oaccrefac.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

public abstract class SourceStatementsCheck<T extends RefactoringParams> extends Check<T> {

    private final IASTStatement[] statements;
    private final IASTNode[] statementsAndComments;
    
    protected SourceStatementsCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
        this.statements = statements;
        this.statementsAndComments = statementsAndComments;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return statements[0].getTranslationUnit();
    }
    
    public IASTStatement[] getStatements() {
        return statements;
    }

    public IASTNode[] getStatementsAndComments() {
        return statementsAndComments;
    }

}
