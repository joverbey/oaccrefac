/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDefaultnoneClauseNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsLoopClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopClauseListNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccKernelsClause;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccKernelsLoopClause;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccParallelClause;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccParallelLoopClause;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;

public class IntroDefaultNoneCheck extends PragmaDirectiveCheck<RefactoringParams> {

    public IntroDefaultNoneCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        super(pragma, statement);
    }
    
    @Override
    protected void doFormCheck(RefactoringStatus status) {
        
        IAccConstruct pragmaAST;
        try {
            pragmaAST = new OpenACCParser().parse(getPragma().getRawSignature());
        } catch (Exception e) {
            status.addFatalError("Selected pragma statement is not a valid OpenACC directive.");
            return;
        }
        
        if (!(pragmaAST instanceof ASTAccParallelLoopNode) && 
                !(pragmaAST instanceof ASTAccParallelNode) &&
                !(pragmaAST instanceof ASTAccKernelsLoopNode) &&
                !(pragmaAST instanceof ASTAccKernelsNode)) {
            status.addFatalError("Cannot add default(none) clause to this type of construct.");
            return;
        }
        
        if(doesACCConstructHaveDefaultNoneClause(pragmaAST, status)) {
            status.addFatalError("Pragma directive already has a default(none) clause.");
            return;
        }
    }
    
    private boolean doesACCConstructHaveDefaultNoneClause(IAccConstruct pragmaAST, RefactoringStatus status) {
        if (pragmaAST instanceof ASTAccParallelLoopNode) {
            return doesParallelLoopNodeHaveDefaultNoneClause((ASTAccParallelLoopNode) pragmaAST);
        }
        else if(pragmaAST instanceof ASTAccParallelNode) {
            return doesParallelNodeHaveDefaultNoneClause((ASTAccParallelNode) pragmaAST); 
        }
        else if(pragmaAST instanceof ASTAccKernelsLoopNode) {
            return doesKernelsLoopNodeHaveDefaultNoneClause((ASTAccKernelsLoopNode) pragmaAST); 
        }
        else if(pragmaAST instanceof ASTAccKernelsNode) {
            return doesKernelsNodeHaveDefaultNoneClause((ASTAccKernelsNode) pragmaAST); 
        }
        else {
            throw new IllegalStateException();
        }
    }
    
    private boolean doesKernelsNodeHaveDefaultNoneClause(ASTAccKernelsNode pragmaAST) {
        if(pragmaAST.getAccKernelsClauseList() == null) {
            return false;
        }
        for (ASTAccKernelsClauseListNode listItem : pragmaAST.getAccKernelsClauseList()) {
            IAccKernelsClause clause = listItem.getAccKernelsClause();
            if (clause instanceof ASTAccDefaultnoneClauseNode) {
                return true;
            }
        }
        return false;
    }

    private boolean doesKernelsLoopNodeHaveDefaultNoneClause(ASTAccKernelsLoopNode pragmaAST) {
        if(pragmaAST.getAccKernelsLoopClauseList() == null) {
            return false;
        }
        for (ASTAccKernelsLoopClauseListNode listItem : pragmaAST.getAccKernelsLoopClauseList()) {
            IAccKernelsLoopClause clause = listItem.getAccKernelsLoopClause();
            if (clause instanceof ASTAccDefaultnoneClauseNode) {
                return true;
            } 
        }
        return false;
    }

    private boolean doesParallelNodeHaveDefaultNoneClause(ASTAccParallelNode pragmaAST) {
        if(pragmaAST.getAccParallelClauseList() == null) {
            return false;
        }
        for (ASTAccParallelClauseListNode listItem : pragmaAST.getAccParallelClauseList()) {
            IAccParallelClause clause = listItem.getAccParallelClause();
            if (clause instanceof ASTAccDefaultnoneClauseNode) {
                return true;
            } 
        }
        return false;
    }

    private boolean doesParallelLoopNodeHaveDefaultNoneClause(ASTAccParallelLoopNode pragmaAST) {
        if(pragmaAST.getAccParallelLoopClauseList() == null) {
            return false;
        }
        for (ASTAccParallelLoopClauseListNode listItem : pragmaAST.getAccParallelLoopClauseList()) {
            IAccParallelLoopClause clause = listItem.getAccParallelLoopClause();
            if (clause instanceof ASTAccDefaultnoneClauseNode) {
                return true;
            } 
        }
        return false;
    }
    
}