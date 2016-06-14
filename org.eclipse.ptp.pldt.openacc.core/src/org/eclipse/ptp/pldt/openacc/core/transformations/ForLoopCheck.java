/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceTestFailure;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class ForLoopCheck<T extends RefactoringParams> extends Check<T> {

    protected final IASTForStatement loop;
    
    protected ForLoopCheck(IASTForStatement loop) {
        this.loop = loop;
    }
    
    protected void doLoopFormCheck(RefactoringStatus status) { 

    }
    
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) { }

    public RefactoringStatus loopFormCheck(RefactoringStatus status, IProgressMonitor pm) {
    	if (containsUnsupportedOp(loop)) {
            status.addError(
                    "Cannot refactor loops containing break, continue, or goto");
        }
    	doLoopFormCheck(status);
        return status;
    }
    
    public RefactoringStatus dependenceCheck(RefactoringStatus status, IProgressMonitor pm) {
        
        IASTStatement[] statements;
        DependenceAnalysis dependenceAnalysis;
        
        statements = ASTUtil.getStatementsIfCompound(loop.getBody());
        
        try {
            dependenceAnalysis = new DependenceAnalysis(pm, statements);
        } catch (DependenceTestFailure e) {
            status.addError("Dependences could not be analyzed.  " + e.getMessage());
            return status;
        }
        
        doDependenceCheck(status, dependenceAnalysis);
        return status;
    }
    
    @Override
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, T params) {
        super.performChecks(status, pm, params);
        if(status.hasFatalError()) {
            return status;
        }
        loopFormCheck(status, pm);
        if(status.hasFatalError()) {
            return status;
        }
        dependenceCheck(status, pm);
        return status;
    }

    public IASTForStatement getLoop() {
        return loop;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return loop.getTranslationUnit();
    }
    
    protected boolean containsUnsupportedOp(IASTForStatement forStmt) {
    	if (!ASTUtil.find(forStmt, IASTGotoStatement.class).isEmpty()) {
    		return true;
    	}
    	List<IASTStatement> ctlFlowStmts = new ArrayList<>();
    	ctlFlowStmts.addAll(ASTUtil.find(forStmt, IASTBreakStatement.class));
    	ctlFlowStmts.addAll(ASTUtil.find(forStmt, IASTContinueStatement.class));
    	for (IASTStatement statement : ctlFlowStmts) {
    		if (ASTUtil.findNearestAncestor(statement, IASTForStatement.class) == forStmt) {
    			if (!insideInnerWhile(statement) && !insideInnerSwitch(statement)) {
    				return true;
    			}
    		} 
    	}

    	return false;
    }
    
    private boolean insideInnerWhile(IASTNode statement) {
    	IASTWhileStatement whileStmt = ASTUtil.findNearestAncestor(statement, IASTWhileStatement.class);
    	if (whileStmt == null) {
    		return false;
    	}
    	IASTForStatement forStmt = ASTUtil.findNearestAncestor(statement, IASTForStatement.class);
    	return !(ASTUtil.findNearestAncestor(forStmt, IASTWhileStatement.class) == whileStmt);
    }
    
    private boolean insideInnerSwitch(IASTNode statement) {
    	IASTSwitchStatement switchStmt = ASTUtil.findNearestAncestor(statement, IASTSwitchStatement.class);
    	if (switchStmt == null) {
    		return false;
    	}
    	IASTForStatement forStmt = ASTUtil.findNearestAncestor(statement, IASTForStatement.class);
    	return !(ASTUtil.findNearestAncestor(forStmt, IASTSwitchStatement.class) == switchStmt);
    }
    
}
