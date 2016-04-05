/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyinClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccCopyoutClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataItemNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;

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

        //TODO modify existing create set as necessary
        //handle parallel loop constructs?
        
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
