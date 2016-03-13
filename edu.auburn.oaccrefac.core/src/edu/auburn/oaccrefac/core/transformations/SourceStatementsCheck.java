package edu.auburn.oaccrefac.core.transformations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
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
    private final IASTNode[] allEnclosedNodes;
    
    //map of each statement with OpenAcc directives to those directives, both as CDT AST nodes and parsed constructs
    Map<IASTStatement, Map<IASTPreprocessorPragmaStatement, IAccConstruct>> accRegions;
    
    protected SourceStatementsCheck(IASTStatement[] statements, IASTNode[] allEnclosedNodes) {
        this.statements = statements;
        this.allEnclosedNodes = allEnclosedNodes;
        this.accRegions = new HashMap<IASTStatement, Map<IASTPreprocessorPragmaStatement, IAccConstruct>>();
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
            Map<IASTPreprocessorPragmaStatement, IAccConstruct> prags = new HashMap<IASTPreprocessorPragmaStatement, IAccConstruct>();
            for(IASTPreprocessorPragmaStatement pragma : ASTUtil.getLeadingPragmas(statement)) {
                try {
                    IAccConstruct con = parser.parse(pragma.getRawSignature());
                    prags.put(pragma, con);
                }
                catch(Exception e) {
                    
                }
            }
            accRegions.put(statement, prags);
        }
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return statements[0].getTranslationUnit();
    }
    
    public IASTStatement[] getStatements() {
        return statements;
    }

    public IASTNode[] getAllEnclosedNodes() {
        return allEnclosedNodes;
    }
    
    public Map<IASTStatement, Map<IASTPreprocessorPragmaStatement, IAccConstruct>> getAccRegions() {
        return accRegions;
    }
    
}
