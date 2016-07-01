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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
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
    		status.addWarning("Data construct will not surround any statements");
    		return;
    	}
		for(IASTStatement stmt : stmts) {
			IASTNode parent = stmt.getParent();
			if(parent instanceof IASTIfStatement) {
				IASTIfStatement ifStmt = (IASTIfStatement) parent; 
				if (ifStmt.getThenClause().equals(stmt) || ifStmt.getElseClause().equals(stmt)) {
					status.addError("Data construct must either be inside the conditional statement or surround both the if statement and its else clause");
				}
			}
		}
    	
    	if(!ASTUtil.doesConstructContainAllReferencesToVariablesItDeclares(stmts)) {
    		status.addError("Construct would surround variable declaration and cause scope errors");
    	}
    	
    	for(IASTStatement stmt : stmts) {
    		if(ASTUtil.find(stmt, IASTIfStatement.class).size() > 0) {
    			status.addWarning("Construct contains a conditional statement; if a variable is conditionally written to and copied out without being copied in, it may result in incorrect values");
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
    		status.addWarning("Resulting data construct cannot do any data transfer");
    	}
    }

    @Override
    public RefactoringStatus performChecks(IProgressMonitor pm, RefactoringParams params) {
    	super.performChecks(pm, params);
    	formCheck();
    	return status;
    }

	private void checkControlFlow(IASTStatement... statements) {
		/*
		 * if there is a control flow statement, it:
		 * must not be labeled
		 * must be within a while/for or must be a break within a switch
		 */
		Set<IASTStatement> ctlFlowStmts = new HashSet<IASTStatement>();
		for (IASTStatement statement : statements) {
			ctlFlowStmts.addAll(ASTUtil.find(statement, IASTBreakStatement.class));
			ctlFlowStmts.addAll(ASTUtil.find(statement, IASTContinueStatement.class));
			ctlFlowStmts.addAll(ASTUtil.find(statement, IASTGotoStatement.class));
		}

		for (IASTStatement ctlFlow : ctlFlowStmts) {
			if (ctlFlow instanceof IASTGotoStatement) {
				status.addError(String.format(
						"Construct will contain goto statement (line %d) that may cause control flow to leave the construct prematurely",
						ctlFlow.getFileLocation().getStartingLineNumber()));
			}
			IASTForStatement f = ASTUtil.findNearestAncestor(ctlFlow, IASTForStatement.class);
			IASTWhileStatement w = ASTUtil.findNearestAncestor(ctlFlow, IASTWhileStatement.class);
			IASTDoStatement d = ASTUtil.findNearestAncestor(ctlFlow, IASTDoStatement.class);
			if (ctlFlow instanceof IASTContinueStatement) {
				if ((f == null || !ASTUtil.isAncestor(f, statements))
						&& (w == null || !ASTUtil.isAncestor(w, statements))
						&& (d == null || !ASTUtil.isAncestor(d, statements))) {
					status.addError(String.format(
							"Construct will contain continue statement (line %d) that may cause control flow to leave the construct prematurely",
							ctlFlow.getFileLocation().getStartingLineNumber()));
				}
			} else if (ctlFlow instanceof IASTBreakStatement) {
				IASTSwitchStatement s = ASTUtil.findNearestAncestor(ctlFlow, IASTSwitchStatement.class);
				if ((f == null || !ASTUtil.isAncestor(f, statements))
						&& (w == null || !ASTUtil.isAncestor(w, statements))
						&& (d == null || !ASTUtil.isAncestor(d, statements))
						&& (s == null || !ASTUtil.isAncestor(s, statements))) {
					status.addError(String.format(
							"Construct will contain break statement (line %d) that may cause control flow to leave the construct prematurely",
							ctlFlow.getFileLocation().getStartingLineNumber()));
				}
			}

		}
	}

}
