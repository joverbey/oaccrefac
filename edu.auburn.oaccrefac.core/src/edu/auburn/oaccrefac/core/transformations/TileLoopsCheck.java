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

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

public class TileLoopsCheck extends ForLoopCheck<TileLoopsParams> {

    private IASTForStatement outer;
    private IASTForStatement inner;
    
    public TileLoopsCheck(IASTForStatement loop) {
        super(loop);
        this.outer = loop;
    }

    @Override
    protected void doParameterCheck(RefactoringStatus status, TileLoopsParams params) {
        
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.loop);
        if (!inq.isPerfectLoopNest()) {
            status.addFatalError("Only perfectly nested loops can be tiled.");
            return;
        }
        
    }
    
    @Override
    protected void doLoopFormCheck(RefactoringStatus status) {
        
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.getLoop());
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (headers.size() < 2) {
            status.addFatalError("There must be two perfectly nested loop to perform loop tiling.");
            return;
        }
        
        inner = ASTUtil.findDepth(outer, IASTForStatement.class, 1);
        
    }

    @Override
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        /**
         * An interchange check is actually a slightly overly conservative tiling check for 2d tiling. 
         * The two are prevented by the same sorts of dependence, but interchange is prevented by ANY 
         * dependences of that type, where tiling may still be legal in some of those cases. However, the
         * cases in which tiling is legal where interchange is not are expected to be uncommon in practice 
         * (they involve having a dependence with a great enough distance vector to always reach to a 
         * previous tile), so this check should be a good choice. 
         */
        InterchangeLoopsCheck check = new InterchangeLoopsCheck(outer, inner);
        check.performChecks(status, new NullProgressMonitor(), new InterchangeLoopParams(1));        
    }

    public IASTForStatement getOuter() {
        return outer;
    }

    public IASTForStatement getInner() {
        return inner;
    }
    
    
    
}
