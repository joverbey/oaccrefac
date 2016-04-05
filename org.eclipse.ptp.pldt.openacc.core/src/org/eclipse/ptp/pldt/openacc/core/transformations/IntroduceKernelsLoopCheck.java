/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

/**
 * IntroduceKernelsLoopCheck checks a for loop for the ability to add an
 * "acc kernels loop" pragma.
 * <p>
 * For the pragma to be added, the loop can't already have an ACC pragma.
 * <p>
 * If the loop has a dependence, a warning is issued.
 * 
 * @author jwowillo
 */
public class IntroduceKernelsLoopCheck extends ForLoopCheck<RefactoringParams> {

    public IntroduceKernelsLoopCheck(final IASTForStatement loop) {
        super(loop);
    }
 
    @Override
    protected void doLoopFormCheck(RefactoringStatus status) {
        if (ForStatementInquisitor.getInquisitor(loop).getPragmas().length > 0) {
            status.addError("This loop contains an ACC pragma.");
        }
    }
    
    @Override
    protected void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        if (dep != null && dep.hasLevel1CarriedDependence()) {
            status.addWarning("This loop may not be parallelizable because it carries a dependence.");
        }
    }
    
}
