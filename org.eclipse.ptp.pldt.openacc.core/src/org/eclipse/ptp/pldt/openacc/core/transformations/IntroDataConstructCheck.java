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
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyinInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyoutInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CreateInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.DataTransferInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitionsAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;
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
    	
    	checkConditionalAssignments(getStatements());
    	
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

    private void checkConditionalAssignments(IASTStatement... statements) {
    	Set<IASTStatement> condStatements = new HashSet<IASTStatement>();
    	for(IASTStatement statement : statements) {
    		condStatements.addAll(ASTUtil.find(statement, IASTIfStatement.class));
    		condStatements.addAll(ASTUtil.find(statement, IASTSwitchStatement.class));
    		condStatements.addAll(ASTUtil.find(statement, IASTWhileStatement.class));
    		condStatements.addAll(ASTUtil.find(statement, IASTForStatement.class));
    	}
    	for(IASTStatement statement : condStatements) {
    		String stmtType = ""; //$NON-NLS-1$
    		Set<IASTName> accesses = new HashSet<IASTName>();
    		if(statement instanceof IASTIfStatement) {
    			stmtType = "if"; //$NON-NLS-1$
    			IASTIfStatement iff = (IASTIfStatement) statement;
    			accesses.addAll(ASTUtil.find(iff.getThenClause(), IASTName.class));
    			if(iff.getElseClause() != null) {
    				accesses.addAll(ASTUtil.find(iff.getElseClause(), IASTName.class));
    			}
    		} else if(statement instanceof IASTSwitchStatement) {
    			stmtType = "switch"; //$NON-NLS-1$
       			IASTSwitchStatement switchh = (IASTSwitchStatement) statement;
    			accesses.addAll(ASTUtil.find(switchh.getBody(), IASTName.class));
    		} else if(statement instanceof IASTWhileStatement) {
    			stmtType = "while"; //$NON-NLS-1$
    			IASTWhileStatement whilee = (IASTWhileStatement) statement;
    			accesses.addAll(ASTUtil.find(whilee.getBody(), IASTName.class));
    		} else if(statement instanceof IASTForStatement) {
    			stmtType = "for"; //$NON-NLS-1$
    			IASTForStatement forr = (IASTForStatement) statement;
    			ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(forr);
    			if(!inq.isCountedLoop() || inq.getLowerBound() == null || inq.getInclusiveUpperBound() == null || inq.getLowerBound() > inq.getInclusiveUpperBound()) {
    				accesses.addAll(ASTUtil.find(forr.getBody(), IASTName.class));
    			}
    		}
    		for(IASTName access : accesses) {
        		CopyinInference copyin = new CopyinInference(statements);
        		CopyoutInference copyout = new CopyoutInference(statements);
        		IASTStatement nearest = OpenACCUtil.findNearestAccConstructAncestor(access);
        		if(nearest == null) nearest = copyin.getRoot();
    			if (ancestorSetContains(copyin, nearest, access.resolveBinding()) && !copyout.contains(nearest, access.resolveBinding())) {
    				status.addWarning(NLS.bind(Messages.IntroDataConstructCheck_ConditionalDefinitionMayRequireAdditionalDataTransfer,
    						new Object[] { stmtType, statement.getFileLocation().getStartingLineNumber() }));
    				break;
    			} else if (ancestorSetContains(copyout, nearest, access.resolveBinding()) && !copyin.contains(nearest, access.resolveBinding())) {
    				status.addWarning(NLS.bind(Messages.IntroDataConstructCheck_ConditionalDefinitionMayRequireAdditionalDataTransfer,
    						new Object[] { stmtType, statement.getFileLocation().getStartingLineNumber() }));
    				break;
    			}
        	}
    	}
    	
	}
    
    private boolean ancestorSetContains(DataTransferInference transfer, IASTStatement nearest, IBinding variable) {
    	if(transfer.contains(transfer.getRoot(), variable)) {
    		return true;
    	}
    	for (IASTStatement ancestor = nearest; ancestor != null; ancestor = OpenACCUtil.findNearestAccConstructAncestor(ancestor)) {
    		if(transfer.contains(ancestor, variable)) {
    			return true;
    		}
		}
    	return false;
    }
    
	private void checkControlFlow(IASTStatement... statements) {
		IASTStatement ctlFlow = ASTUtil.getUnsupportedOp(statements);
		if (ctlFlow instanceof IASTGotoStatement) {
			status.addError(String.format(
					Messages.IntroDataConstructCheck_WillContainGotoStatement,
					ctlFlow.getFileLocation().getStartingLineNumber()));
		}
		else if (ctlFlow instanceof IASTReturnStatement) {
			status.addError(String.format(
					Messages.IntroDataConstructCheck_WillContainReturnStatement,
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
