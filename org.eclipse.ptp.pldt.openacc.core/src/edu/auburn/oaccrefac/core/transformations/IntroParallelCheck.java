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
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class IntroParallelCheck extends ForLoopCheck<RefactoringParams> {

    public IntroParallelCheck(final IASTForStatement loop) {
        super(loop);
    }

    @Override
    protected void doLoopFormCheck(RefactoringStatus status) {
        checkPragma(status);
    }

    @Override
    public void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        if (dep != null && dep.hasLevel1CarriedDependence()) {
            status.addError("This loop cannot be parallelized because it carries a dependence.");
        }
    }
    
    private void checkPragma(RefactoringStatus status) {
        ForStatementInquisitor loop1 = InquisitorFactory.getInquisitor(loop);
        if(loop1.getPragmas().length != 0) {
            status.addFatalError("When a loop has a pragma associated with it, it cannot have another pragma added to it.");
        }
    }

}
