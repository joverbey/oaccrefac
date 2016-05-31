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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyin;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyout;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCreate;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

public class IntroDataConstructCheck extends SourceStatementsCheck<RefactoringParams> {

    public IntroDataConstructCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
        super(statements, statementsAndComments);
    }
    
    protected void formCheck(RefactoringStatus status) {
    	IASTStatement[] stmts = getStatements();
    	if(stmts.length < 1) {
    		status.addWarning("Data construct will not surround any statements");
    		return;
    	}
    	else {
    		for(IASTStatement stmt : stmts) {
    			IASTNode parent = stmt.getParent();
    			if(parent instanceof IASTIfStatement) {
    				IASTIfStatement ifStmt = (IASTIfStatement) parent; 
    				if (ifStmt.getThenClause().equals(stmt) || ifStmt.getElseClause().equals(stmt)) {
    					status.addError("Data construct must either be inside the conditional statement or surround both the if statement and its else clause");
    					return;
    				}
    			}
    		}
    	}
    	
    	IASTFunctionDefinition func = ASTUtil.findNearestAncestor(stmts[0], IASTFunctionDefinition.class);
    	List<IASTName> varOccurrences = ASTUtil.find(func, IASTName.class);
    	Set<IASTDeclarator> declaratorsInConstruct = new HashSet<IASTDeclarator>();
    	for(IASTStatement stmt : stmts) {
    		for(IASTSimpleDeclaration declaration : ASTUtil.find(stmt, IASTSimpleDeclaration.class)) {
    			//if declarator.getName().resolveBinding() occurs outside the construct
    			declaratorsInConstruct.addAll(Arrays.asList(declaration.getDeclarators()));
        	}
    	}
    	for(IASTDeclarator declarator : declaratorsInConstruct) {
    		for(IASTName occurrence : varOccurrences) {
    			//if a variable is declared in the construct and this is some use of it
    			if(declarator.getName().resolveBinding().equals(occurrence.resolveBinding())) {
    				if(!ASTUtil.isAncestor(occurrence, stmts)) {
    					status.addError("Construct would surround variable declaration and cause scope errors");
    					return;
    				}
    			}
    		}
    	}
    }
    
    @Override
    protected void doReachingDefinitionsCheck(RefactoringStatus status, ReachingDefinitions rd) {
    	Map<IASTStatement, Set<IBinding>> copyin = new InferCopyin(rd, getStatements()).get();
    	Map<IASTStatement, Set<IBinding>> copyout = new InferCopyout(rd, getStatements()).get();
    	Map<IASTStatement, Set<IBinding>> create = new InferCreate(rd, getStatements()).get();
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
    		status.addError("Resulting data construct cannot do any data transfer");
    	}
    }

    @Override
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, RefactoringParams params) {
    	super.performChecks(status, pm, params);
    	formCheck(status);
    	return status;
    }

}