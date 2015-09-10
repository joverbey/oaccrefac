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
package edu.auburn.oaccrefac.core.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DataDependence;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.core.dependence.DependenceType;
import edu.auburn.oaccrefac.core.dependence.Direction;
import edu.auburn.oaccrefac.core.dependence.FusionDependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

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
        }
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
        return (ASTUtil.isAncestorOf(l1, s1) && ASTUtil.isAncestorOf(l2, s2)) || 
                (ASTUtil.isAncestorOf(l1, s2) && ASTUtil.isAncestorOf(l2, s1));
    }
    
}
