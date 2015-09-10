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

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;

public class DistributeLoopsCheck extends ForLoopCheck<RefactoringParams> {

    public DistributeLoopsCheck(IASTForStatement loop) {
        super(loop);
    }

    @Override
    public void doLoopFormCheck(RefactoringStatus status) {
        // If the loop doesn't have children, bail.
        if (!(loop.getBody() instanceof IASTCompoundStatement)) {
            status.addFatalError("Body does not have any statements, so loop fission is useless.");
        }

        if (loop.getBody().getChildren().length < 2) {
            status.addFatalError("Loop fission refactoring requires more than one statement.");
        }
    }

    @Override
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        //TODO figure out how to do this dependence analysis
    }
    
}
