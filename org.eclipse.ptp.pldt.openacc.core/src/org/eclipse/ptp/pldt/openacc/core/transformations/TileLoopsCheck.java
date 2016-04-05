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

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class TileLoopsCheck extends ForLoopCheck<TileLoopsParams> {

    private IASTForStatement outer;
    private IASTForStatement inner;
    
    public TileLoopsCheck(IASTForStatement loop) {
        super(loop);
        this.outer = loop;
    }

    @Override
    protected void doParameterCheck(RefactoringStatus status, TileLoopsParams params) {
        
        if(params.getHeight() < 1) {
            status.addFatalError("Height must be at least 1");
            return;
        }
        
        if(params.getWidth() < 1) {
            status.addFatalError("Width must be at least 1");
            return;
        }
        
    }
    
    @Override
    protected void doLoopFormCheck(RefactoringStatus status) {
        
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.getLoop());
        
        // For now, we will disallow pragmas on loops to be tiled. This is 
        // of complexity in placing the pragma on any of the four loops 
        // created by tiling. Some dependencies that are allowed with tiling
        // are not allowed by pragmas.
        
        if (inq.getPragmas().length > 0) {
            status.addError("This loop contains an ACC pragma.");
        }

        if (!inq.isPerfectLoopNest(1)) {
            status.addFatalError("Only perfectly nested loops can be tiled.");
            return;
        }
        
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (headers.size() < 2) {
            status.addFatalError("There must be two nested loops to perform loop tiling.");
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
         * 
         * TODO only really need to do a check for dependence, not a full performChecks() call, but 
         * that can't be done since the inner loop in the interchange check is discovered elsewhere
         */
        
        new InterchangeLoopsCheck(outer).performChecks(status, new NullProgressMonitor(), new InterchangeLoopParams(1));        
    }

    public IASTForStatement getOuter() {
        return outer;
    }

    public IASTForStatement getInner() {
        return inner;
    }
    
    
    
}