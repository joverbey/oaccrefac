/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     William Hester (Auburn) - Decouple SourceStatementsCheck and
 *     			IntroduceDataConstructCheck
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyinInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyoutInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CreateInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitionsAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

public class IntroDataConstructCheck extends SourceStatementsCheck<RefactoringParams> {

    public IntroDataConstructCheck(RefactoringStatus status, IASTStatement[] statements, IASTNode[] statementsAndComments) {
        super(status, statements, statementsAndComments);
    }
    
    protected void formCheck() {
    	IASTStatement[] stmts = getStatements();
    	if(stmts.length < 1) {
    		status.addWarning(Messages.IntroDataConstructCheck_WillNotSurroundAnyStatements);
    		return;
    	}
		for(IASTStatement stmt : stmts) {
			IASTNode parent = stmt.getParent();
			if(parent instanceof IASTIfStatement) {
				IASTIfStatement ifStmt = (IASTIfStatement) parent; 
				if (ifStmt.getThenClause().equals(stmt) || ifStmt.getElseClause().equals(stmt)) {
					status.addError(Messages.IntroDataConstructCheck_MustBeInConditionalOrSurroundIfAndElse);
				}
			}
		}
    	
    	if(!ASTUtil.doesConstructContainAllReferencesToVariablesItDeclares(stmts)) {
    		status.addError(Messages.IntroDataConstructCheck_WouldSurroundDeclarationScopeErrors);
    	}
    	
    	for(IASTStatement stmt : stmts) {
    		if(ASTUtil.find(stmt, IASTIfStatement.class).size() > 0) {
    			status.addWarning(Messages.IntroDataConstructCheck_ConditionalWriteMayCauseIncorrectDataTransfer);
    			break;
    		}
    	}
    	
    	checkControlFlow(getStatements());
    	
    }
    
	@Override
    public RefactoringStatus doCheck(IProgressMonitor pm) {
        doReachingDefinitionsCheck(ReachingDefinitionsAnalysis.forFunction(ASTUtil.findNearestAncestor(getStatements()[0], IASTFunctionDefinition.class)));
        return status;
    }

    protected void doReachingDefinitionsCheck(ReachingDefinitionsAnalysis rd) {
    	Map<IASTStatement, Set<IBinding>> copyin = new CopyinInference(getStatements()).get();
    	Map<IASTStatement, Set<IBinding>> copyout = new CopyoutInference(getStatements()).get();
    	Map<IASTStatement, Set<IBinding>> create = new CreateInference(getStatements()).get();
    	boolean allRootsEmpty = true;
    	for(IASTStatement statement : copyin.keySet()) {
    		if(statement instanceof ArbitraryStatement && !copyin.get(statement).isEmpty()) {
    			allRootsEmpty = false;
    		}
    	}
    	for(IASTStatement statement : copyout.keySet()) {
    		if(statement instanceof ArbitraryStatement && !copyout.get(statement).isEmpty()) {
    			allRootsEmpty = false;
    		}
    	}
    	for(IASTStatement statement : create.keySet()) {
    		if(statement instanceof ArbitraryStatement && !create.get(statement).isEmpty()) {
    			allRootsEmpty = false;
    		}
    	}
    	if(allRootsEmpty) {
    		status.addWarning(Messages.IntroDataConstructCheck_NoDataTransfer);
    	}
    }

    @Override
    public RefactoringStatus performChecks(IProgressMonitor pm, RefactoringParams params) {
    	super.performChecks(pm, params);
    	formCheck();
    	return status;
    }

	private void checkControlFlow(IASTStatement... statements) {
		IASTStatement ctlFlow = ASTUtil.getUnsupportedOp(statements);
		if (ctlFlow instanceof IASTGotoStatement) {
			status.addError(String.format(
					Messages.IntroDataConstructCheck_WillContainGotoStatement,
					ctlFlow.getFileLocation().getStartingLineNumber()));
		} else if (ctlFlow instanceof IASTContinueStatement) {
			status.addError(String.format(
					Messages.IntroDataConstructCheck_WillContainBadContinue,
					ctlFlow.getFileLocation().getStartingLineNumber()));
		} else if (ctlFlow instanceof IASTBreakStatement) {
			status.addError(String.format(
					Messages.IntroDataConstructCheck_WillContainBadBreak,
					ctlFlow.getFileLocation().getStartingLineNumber()));
		} else if (ctlFlow instanceof IASTReturnStatement) {
			status.addError(String.format(
					Messages.IntroDataConstructCheck_WillContainReturnStatement,
					ctlFlow.getFileLocation().getStartingLineNumber()));
	    }
	}

}
