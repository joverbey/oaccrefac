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
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DataDependence;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceTestFailure;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceType;
import org.eclipse.ptp.pldt.openacc.core.dependence.Direction;
import org.eclipse.ptp.pldt.openacc.core.dependence.FusionDependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.eclipse.ptp.pldt.openacc.internal.core.InquisitorFactory;
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
            status.addFatalError("There is no for loop for fusion to be possible.");
            return;
        }
        //breaks test 02 because only one loop
        checkPragma(status);
    }
  
    @Override
    public RefactoringStatus dependenceCheck(RefactoringStatus status, IProgressMonitor pm) {
        
        ForStatementInquisitor loop = InquisitorFactory.getInquisitor(first);
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
        ForStatementInquisitor loop1 = InquisitorFactory.getInquisitor(first);
        ForStatementInquisitor loop2 = InquisitorFactory.getInquisitor(second);
        if(comparePragma() && (loop1.getPragmas().length != 0 && loop2.getPragmas().length != 0 || loop1.getPragmas().length != loop2.getPragmas().length)) {
                    status.addFatalError("When a loop has a pragma associated with it, it cannot be fused unless both loops have identical pragmas.");
        }
    }
    
    //if both pragmas are not identical returns true
    private boolean comparePragma(){
        ForStatementInquisitor loop1 = InquisitorFactory.getInquisitor(first);
        ForStatementInquisitor loop2 = InquisitorFactory.getInquisitor(second);
        String loop1String;
        String loop2String;
        loop1String = Arrays.toString(loop1.getPragmas());
        loop2String = Arrays.toString(loop2.getPragmas());
        boolean loopBool = !loop1String.equals(loop2String); 
        return loopBool;
    }
}