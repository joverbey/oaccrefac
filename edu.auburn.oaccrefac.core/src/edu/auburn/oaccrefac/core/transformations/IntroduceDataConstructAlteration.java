package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

import edu.auburn.oaccrefac.core.dataflow.ReachingDefinitions;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyinClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccCopyoutClauseNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataItemNode;
import edu.auburn.oaccrefac.core.parser.ASTAccDataNode;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccKernelsNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelClauseListNode;
import edu.auburn.oaccrefac.core.parser.ASTAccParallelNode;
import edu.auburn.oaccrefac.core.parser.IAccConstruct;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.OpenACCUtil;

public class IntroduceDataConstructAlteration extends SourceStatementsAlteration<IntroduceDataConstructCheck> {

    public IntroduceDataConstructAlteration(IASTRewrite rewriter, IntroduceDataConstructCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() throws Exception {
        IASTStatement[] stmts = getStatements();
        ReachingDefinitions rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(stmts[0], IASTFunctionDefinition.class));
        Set<String> copyin = OpenACCUtil.inferCopyin(rd, getStatements());
        Set<String> copyout = OpenACCUtil.inferCopyout(rd, getStatements());

        //TODO remove vars in copyin/copyout from the existing copyin/out sets
        //also modify existing create set as necessary
        
        /*
         * change existing constructs' clauses
         *     - may need to just rebuild the whole pragma in its own string
         * insert new pragma and LCURLY at the start
         * insert RCURLY at the end
         */
        StringBuilder newPragma = new StringBuilder(pragma("acc data"));
        if(!copyin.isEmpty()) {
            newPragma.append(" ");
            newPragma.append(copyin(copyin.toArray(new String[copyin.size()])));
        }
        if(!copyout.isEmpty()) {
            newPragma.append(" ");
            newPragma.append(copyout(copyout.toArray(new String[copyout.size()])));
        }
        
        //TODO make SourceStatementsRefactoring aware of pragmas in the selection as well as comments
        replaceContainedPragmas(copyin, copyout);
        this.insertBefore(getAllEnclosedNodes()[0], newPragma.toString() + NL + LCURLY);
//        this.insertBefore(getAllEnclosedNodes()[0], NL);
//        this.insertBefore(getAllEnclosedNodes()[0], LCURLY);
        this.insertAfter(getAllEnclosedNodes()[getAllEnclosedNodes().length - 1], RCURLY);

        finalizeChanges();
    }
    
    private void replaceContainedPragmas(Set<String> copyin, Set<String> copyout) {
        
        for(IASTStatement statement : check.getAccRegions().keySet()) {
            for(IASTPreprocessorPragmaStatement pragma : check.getAccRegions().get(statement).keySet()) {
                IAccConstruct con = check.getAccRegions().get(statement).get(pragma);
                if(con instanceof ASTAccParallelNode) {
                    StringBuilder newPragma = new StringBuilder(pragma("acc parallel"));
                    newPragma.append(" ");
                    newPragma.append(getParallelClauseList((ASTAccParallelNode) con, copyin, copyout));
                    this.replace(pragma, newPragma.toString());
                }
                else if(con instanceof ASTAccKernelsNode) {
                    StringBuilder newPragma = new StringBuilder(pragma("acc kernels"));
                    newPragma.append(" ");
                    newPragma.append(getKernelsClauseList((ASTAccKernelsNode) con, copyin, copyout));
                    this.replace(pragma, newPragma.toString());
                } 
                else if (con instanceof ASTAccDataNode) {
                    StringBuilder newPragma = new StringBuilder(pragma("acc data"));
                    newPragma.append(" ");
                    newPragma.append(getDataClauseList((ASTAccDataNode) con, copyin, copyout));
                    this.replace(pragma, newPragma.toString());
                }
                
            }
        }
        
    }
    
    private String getParallelClauseList(ASTAccParallelNode par, Set<String> copyin, Set<String> copyout) {
        StringBuilder clauseList = new StringBuilder();
        for(ASTAccParallelClauseListNode clause : par.getAccParallelClauseList()) {
            if(clause.getAccParallelClause() instanceof ASTAccCopyinClauseNode) {
                ASTAccCopyinClauseNode copyinClause = (ASTAccCopyinClauseNode) clause.getAccParallelClause();
                List<String> vars = new ArrayList<String>();
                for(ASTAccDataItemNode item : copyinClause.getAccDataList()) {
                    if(!copyin.contains(item.getIdentifier().getIdentifier().getText())) {
                        vars.add(item.toString());
                    }
                }
                if(!vars.isEmpty()) {
                    clauseList.append(copyin(vars.toArray(new String[vars.size()])));
                    clauseList.append(" ");
                }
            }
            else if(clause.getAccParallelClause() instanceof ASTAccCopyoutClauseNode) {
                ASTAccCopyoutClauseNode copyoutClause = (ASTAccCopyoutClauseNode) clause.getAccParallelClause();
                List<String> vars = new ArrayList<String>();
                for(ASTAccDataItemNode item : copyoutClause.getAccDataList()) {
                    if(!copyin.contains(item.getIdentifier().getIdentifier().getText())) {
                        vars.add(item.toString());
                    }
                }
                if(!vars.isEmpty()) {
                    clauseList.append(copyout(vars.toArray(new String[vars.size()])));
                    clauseList.append(" ");
                }
            }
            else {
                clauseList.append(clause);
                clauseList.append(" ");
            }
        }
        return clauseList.toString();
    }
    
    private String getKernelsClauseList(ASTAccKernelsNode ker, Set<String> copyin, Set<String> copyout) {
        StringBuilder clauseList = new StringBuilder();
        for(ASTAccKernelsClauseListNode clause : ker.getAccKernelsClauseList()) {
            if(clause.getAccKernelsClause() instanceof ASTAccCopyinClauseNode) {
                ASTAccCopyinClauseNode copyinClause = (ASTAccCopyinClauseNode) clause.getAccKernelsClause();
                List<String> vars = new ArrayList<String>();
                for(ASTAccDataItemNode item : copyinClause.getAccDataList()) {
                    if(!copyin.contains(item.getIdentifier().getIdentifier().getText())) {
                        vars.add(item.toString());
                    }
                }
                clauseList.append(copyin(vars.toArray(new String[vars.size()])));
                clauseList.append(" ");
            }
            else if(clause.getAccKernelsClause() instanceof ASTAccCopyoutClauseNode) {
                ASTAccCopyoutClauseNode copyoutClause = (ASTAccCopyoutClauseNode) clause.getAccKernelsClause();
                List<String> vars = new ArrayList<String>();
                for(ASTAccDataItemNode item : copyoutClause.getAccDataList()) {
                    if(!copyin.contains(item.getIdentifier().getIdentifier().getText())) {
                        vars.add(item.toString());
                    }
                }
                clauseList.append(copyout(vars.toArray(new String[vars.size()])));
                clauseList.append(" ");
            }
            else {
                clauseList.append(clause);
                clauseList.append(" ");
            }
        }
        return clauseList.toString();
    }

    private String getDataClauseList(ASTAccDataNode data, Set<String> copyin, Set<String> copyout) {
        StringBuilder clauseList = new StringBuilder();
        for(ASTAccDataClauseListNode clause : data.getAccDataClauseList()) {
            if(clause.getAccDataClause() instanceof ASTAccCopyinClauseNode) {
                ASTAccCopyinClauseNode copyinClause = (ASTAccCopyinClauseNode) clause.getAccDataClause();
                List<String> vars = new ArrayList<String>();
                for(ASTAccDataItemNode item : copyinClause.getAccDataList()) {
                    if(!copyin.contains(item.getIdentifier().getIdentifier().getText())) {
                        vars.add(item.toString());
                    }
                }
                clauseList.append(copyin(vars.toArray(new String[vars.size()])));
                clauseList.append(" ");
            }
            else if(clause.getAccDataClause() instanceof ASTAccCopyoutClauseNode) {
                ASTAccCopyoutClauseNode copyoutClause = (ASTAccCopyoutClauseNode) clause.getAccDataClause();
                List<String> vars = new ArrayList<String>();
                for(ASTAccDataItemNode item : copyoutClause.getAccDataList()) {
                    if(!copyin.contains(item.getIdentifier().getIdentifier().getText())) {
                        vars.add(item.toString());
                    }
                }
                clauseList.append(copyout(vars.toArray(new String[vars.size()])));
                clauseList.append(" ");
            }
            else {
                clauseList.append(clause);
                clauseList.append(" ");
            }
        }
        return clauseList.toString();
    }
}
