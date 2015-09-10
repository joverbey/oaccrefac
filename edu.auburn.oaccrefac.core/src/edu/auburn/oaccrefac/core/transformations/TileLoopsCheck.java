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

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

public class TileLoopsCheck extends ForLoopCheck<TileLoopsParams> {

    public TileLoopsCheck(IASTForStatement loop) {
        super(loop);
    }

    @Override
    protected void doParameterCheck(RefactoringStatus status, TileLoopsParams params) {
        
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.loop);
        if (!inq.isPerfectLoopNest()) {
            status.addFatalError("Only perfectly nested loops can be tiled.");
            return;
        }

        if (params.getPropagate() > params.getDepth()) {
            status.addWarning(
                    "Warning: propagation higher than params.getDepth() -- propagation " + "will occur as many times as possible.");
            return;
        }

        // TODO -- make this better (this stuff is from strip mining-specific code)
        if (params.getStripFactor() <= 0) {
            status.addFatalError("Invalid strip factor (<= 0).");
            return;
        }

        if (params.getDepth() < 0 || params.getDepth() >= inq.getPerfectLoopNestHeaders().size()) {
            status.addFatalError("There is no for-loop at params.getDepth() " + params.getDepth());
            return;
        }

        int iterator = inq.getIterationFactor(params.getDepth());
        if (params.getStripFactor() % iterator != 0 || params.getStripFactor() <= iterator) {
            status.addFatalError("Strip mine factor must be greater than and "
                    + "divisible by the intended loop's iteration factor.");
            return;
        }
    }

    @Override
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        // TODO dependence analysis??? how to do i dunno

        // DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(status, pm);
        // if (dependenceAnalysis != null && dependenceAnalysis.()) {
        // status.addError("This loop cannot be parallelized because it carries a dependence.",
        // getLocation(getLoop()));
        // }
    }
    
}
