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
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.Arrays;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceTestFailure;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ASTMatcher;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

public class FuseLoopsCheck extends ForLoopCheck<RefactoringParams> {

    private IASTForStatement first;
    private IASTForStatement second;

    public FuseLoopsCheck(IASTForStatement first) {
        super(first);
        this.first = this.loop;
        
        // This gets the selected loop to re-factor.
        IASTNode next = ASTUtil.getNextSibling(first);

        // Create pattern for matching loop headers
        IASTForStatement pattern = first.copy();
        pattern.setBody(new ArbitraryStatement());

        if (next != null && next instanceof IASTForStatement) {

            // Check to make sure the first and second loops have same headers
            if (ASTMatcher.unify(pattern, next) != null) {
                second = (IASTForStatement) next;
            } else {
                second = null;
            }
        }
        else {
            second = null;
        }
    }

    public IASTForStatement getLoop1() {
        return this.first;
    }
    
    public IASTForStatement getLoop2() {
        return this.second;
    }
    
    @Override
    public void doLoopFormCheck(RefactoringStatus status) {
        if (second == null) {
            status.addFatalError(Messages.FuseLoopsCheck_MustBeTwoLoops);
            return;
        }
        
        IASTName conflict = getNameConflict(first, second);
        if(conflict != null) {
        	status.addError(String.format(Messages.FuseLoopsCheck_DefinitionsMayConflict, conflict.getRawSignature(), conflict.getRawSignature()));
            return;
        }
        
        //breaks test 02 because only one loop
        checkPragma(status);
    }
    
    private IASTName getNameConflict(IASTForStatement first, IASTForStatement second) {
    	for(IASTDeclarator decl : ASTUtil.find(first.getBody(), IASTDeclarator.class)) {
    		for(IASTName name : ASTUtil.find(second.getBody(), IASTName.class)) {
    			if(decl.getName().getRawSignature().equals(name.getRawSignature())
    					&& varWillShadow(name.resolveBinding(), second)) {
    				return decl.getName();
    			}
    		}
    	}
    	return null;
    }
    
    private boolean varWillShadow(IBinding var, IASTForStatement second) {
    	IASTStatement body = second.getBody();
    	for(IASTDeclarator decl : ASTUtil.find(second.getBody(), IASTDeclarator.class)) {
    		IASTDeclarationStatement declStmt = ASTUtil.findNearestAncestor(decl, IASTDeclarationStatement.class);
    		if(decl.getName().resolveBinding().equals(var)) {
    			if(body.equals(declStmt) || 
        				(body instanceof IASTCompoundStatement 
            					&& Arrays.asList(((IASTCompoundStatement) body).getStatements()).contains(declStmt))) {
    				return true;
    			}
    			else {
    				return false;
    			}
    		}
    	}
		return true;
	}
    
    @Override
	public RefactoringStatus dependenceCheck(RefactoringStatus status, IProgressMonitor pm) {
		IASTForStatement firstCopy = first.copy(CopyStyle.withLocations);
		IASTForStatement secondCopy = second.copy(CopyStyle.withLocations);
		firstCopy.setParent(ASTUtil.findNearestAncestor(first, IASTFunctionDefinition.class).getBody());
		ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
		IASTCompoundStatement newBody = factory.newCompoundStatement();

		newBody.addStatement(firstCopy.getBody());
		newBody.addStatement(secondCopy.getBody());
		firstCopy.setBody(newBody);
		newBody.setParent(firstCopy);

		IASTStatement[] statements;
		DependenceAnalysis dependenceAnalysis;

		// Analysis of fused loop
		statements = ASTUtil.getStatementsIfCompound(newBody);
		try {
			dependenceAnalysis = new DependenceAnalysis(pm, statements);
		} catch (DependenceTestFailure e) {
			status.addError(Messages.FuseLoopsCheck_DependencesNotAnalyzed + e.getMessage());
			return status;
		}

		// Analysis of first loop
		IASTStatement[] firstStatements;
		DependenceAnalysis firstDependenceAnalysis;
		firstStatements = ASTUtil.getStatementsIfCompound(first.getBody());
		try {
			firstDependenceAnalysis = new DependenceAnalysis(pm, firstStatements);
		} catch (DependenceTestFailure e) {
			status.addError(Messages.FuseLoopsCheck_DependencesNotAnalyzed + e.getMessage());
			return status;
		}

		// Analysis of second loop
		IASTStatement[] secondStatements;
		DependenceAnalysis secondDependenceAnalysis;
		secondStatements = ASTUtil.getStatementsIfCompound(second.getBody());
		try {
			secondDependenceAnalysis = new DependenceAnalysis(pm, secondStatements);
		} catch (DependenceTestFailure e) {
			status.addError(Messages.FuseLoopsCheck_DependencesNotAnalyzed + e.getMessage());
			return status;
		}

		if (dependenceAnalysis.carryDependenceCount() != (firstDependenceAnalysis.carryDependenceCount() +
				secondDependenceAnalysis.carryDependenceCount())) {
			status.addError(Messages.FuseLoopsCheck_FusionCreatesDependence);
		}

		return status;
    }
    
    private void checkPragma(RefactoringStatus status) {
        int pragmasOnFirst = ASTUtil.getPragmaNodes(first).size();
        int pragmasOnSecond = ASTUtil.getPragmaNodes(second).size();
        boolean empty = (pragmasOnFirst == 0 && pragmasOnSecond == 0);
        if (!empty && pragmasDiffer()) {
            status.addFatalError(Messages.FuseLoopsCheck_CannotFuseDifferentPragmas);
        }
    }
    
    private boolean pragmasDiffer(){
    	// FIXME: Why are we comparing strings?
        String loop1String = Arrays.toString(ASTUtil.getPragmaStrings(first));
        String loop2String = Arrays.toString(ASTUtil.getPragmaStrings(second));
        return !loop1String.equals(loop2String); 
    }
}