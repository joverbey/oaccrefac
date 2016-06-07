/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DataDependence;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceTestFailure;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceType;
import org.eclipse.ptp.pldt.openacc.core.dependence.Direction;
import org.eclipse.ptp.pldt.openacc.core.dependence.FusionDependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
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
            status.addFatalError("There is there must be two for loops fusion to be possible.");
            return;
        }
        
        IASTName conflict = getNameConflict(first, second);
        if(conflict != null) {
        	status.addError(String.format("A definition of \"%s\" in the first loop may shadow \"%s\" used in the second loop", conflict.getRawSignature(), conflict.getRawSignature()));
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
        
        ForStatementInquisitor loop = ForStatementInquisitor.getInquisitor(first);
        FusionDependenceAnalysis dep;
        
        try {
            dep = new FusionDependenceAnalysis(pm, 
                    loop.getIndexVariable(), loop.getLowerBound(), loop.getInclusiveUpperBound(), 
                    getStatementsFromLoopBodies(first, second));
        } catch (DependenceTestFailure e) {
            status.addError("Dependences could not be analyzed.  " + e.getMessage());
            return status;
        }
        
        for(DataDependence d : dep.getDependences()) {

            if(d.isLoopCarried() &&
                    statementsComeFromDifferentLoops(second, first, d.getStatement1(), d.getStatement2()) &&
                    (d.getDirectionVector()[d.getLevel()-1] == Direction.LT || d.getDirectionVector()[d.getLevel()-1] == Direction.LE) &&
                    d.getType() == DependenceType.ANTI)  {
                
                status.addError("A dependence in the loops is fusion-preventing");
                
            }
            
        }
        return status;
    }
    
    private IASTStatement[] getStatementsFromLoopBodies(IASTForStatement l1, IASTForStatement l2) {
        List<IASTStatement> stmts = new ArrayList<IASTStatement>();
        
        if(l1.getBody() instanceof IASTCompoundStatement) {
            IASTStatement[] bodyStmts = ((IASTCompoundStatement) l1.getBody()).getStatements();
            stmts.addAll(Arrays.asList(bodyStmts));
        }
        else {
            stmts.add(l1.getBody());
        }
        
        if(l2.getBody() instanceof IASTCompoundStatement) {
            IASTStatement[] bodyStmts = ((IASTCompoundStatement) l2.getBody()).getStatements();
            stmts.addAll(Arrays.asList(bodyStmts));
        }
        else {
            stmts.add(l2.getBody());
        }
        
        return stmts.toArray(new IASTStatement[stmts.size()]);
    }
    
    private static boolean statementsComeFromDifferentLoops(IASTForStatement l1, IASTForStatement l2, IASTStatement s1, IASTStatement s2) {
        return (ASTUtil.isAncestor(s1, l1) && ASTUtil.isAncestor(s2, l2)) || 
                (ASTUtil.isAncestor(s1, l2) && ASTUtil.isAncestor(s2, l1));
    }
    
    private void checkPragma(RefactoringStatus status) {
        int pragmasOnFirst = ASTUtil.getPragmaNodes(first).size();
        int pragmasOnSecond = ASTUtil.getPragmaNodes(second).size();
        boolean empty = (pragmasOnFirst == 0 && pragmasOnSecond == 0);
        if (!empty && pragmasDiffer()) {
            status.addFatalError("When a loop has a pragma associated with it, it cannot be fused unless both loops have identical pragmas.");
        }
    }
    
    private boolean pragmasDiffer(){
    	// FIXME: Why are we comparing strings?
        String loop1String = Arrays.toString(ASTUtil.getPragmaStrings(first));
        String loop2String = Arrays.toString(ASTUtil.getPragmaStrings(second));
        return !loop1String.equals(loop2String); 
    }
}