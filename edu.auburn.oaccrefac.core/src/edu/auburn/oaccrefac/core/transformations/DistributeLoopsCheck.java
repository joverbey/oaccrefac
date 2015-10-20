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

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DataDependence;
import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceType;
import edu.auburn.oaccrefac.core.dependence.Direction;

public class DistributeLoopsCheck extends ForLoopCheck<RefactoringParams> {

    public DistributeLoopsCheck(IASTForStatement loop) {
        super(loop);
    }

    @Override
    public void doLoopFormCheck(RefactoringStatus status) {
        // If the loop doesn't have children, bail.
        if (!(loop.getBody() instanceof IASTCompoundStatement)) {
            status.addFatalError("Body is not compound, so fission cannot be performed.");
            return;
        }

        if (loop.getBody().getChildren().length < 2) {
            status.addFatalError("Fission requires more than one statement.");
            return;
        }
    }

    @Override
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        for (DataDependence d : dep.getDependences()) {
            // if there is a loop-carried anti-dependence in the less-than direction
            if (d.isLoopCarried()
                    && (d.getDirectionVector()[d.getLevel() - 1] == Direction.LT
                            || d.getDirectionVector()[d.getLevel() - 1] == Direction.LE)
                    && d.getType() == DependenceType.ANTI) {

                status.addError("A dependence in the loops is fission-preventing");

            }

        }
    }
    
}
