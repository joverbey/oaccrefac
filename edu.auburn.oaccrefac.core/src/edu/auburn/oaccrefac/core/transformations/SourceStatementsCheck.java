package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.parser.IAccConstruct;
import edu.auburn.oaccrefac.core.parser.OpenACCParser;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

public abstract class SourceStatementsCheck<T extends RefactoringParams> extends Check<T> {

    private final IASTStatement[] statements;
    private final IASTNode[] statementsAndComments;
    Map<IASTStatement, List<IAccConstruct>> accRegions = new HashMap<IASTStatement, List<IAccConstruct>>();
    
    protected SourceStatementsCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
        this.statements = statements;
        this.statementsAndComments = statementsAndComments;
    }
    
    public RefactoringStatus reachingDefinitionsCheck(RefactoringStatus status, IProgressMonitor pm) {
        doReachingDefinitionsCheck(status, new ReachingDefinitions(ASTUtil.findNearestAncestor(getStatements()[0], IASTFunctionDefinition.class)));
        return status;
    }
    
    protected abstract void doReachingDefinitionsCheck(RefactoringStatus status, ReachingDefinitions rd);
    
    @Override
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, T params) {
        super.performChecks(status, pm, params);
        if(status.hasFatalError()) {
            return status;
        }
        reachingDefinitionsCheck(status, pm);
        return status;
    }

    protected final void populateAccMap() {
        OpenACCParser parser = new OpenACCParser();
        for(IASTStatement statement : getStatements()) {
            List<IAccConstruct> cons = new ArrayList<IAccConstruct>();
            for(String pragma : ASTUtil.getPragmas(statement)) {
                try {
                    cons.add(parser.parse(pragma));
                }
                catch(Exception e) {
                    
                }
            }
            accRegions.put(statement, cons);
        }
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
    
    public Map<IASTStatement, List<IAccConstruct>> getOpenAccRegions() {
        return accRegions;
    }

}
