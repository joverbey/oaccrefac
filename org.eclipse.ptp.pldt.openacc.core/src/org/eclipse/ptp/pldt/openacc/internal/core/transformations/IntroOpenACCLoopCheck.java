/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Jacob Neeley (Auburn) - initial API and implementation
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DataDependence;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.ASTAccKernelsLoopNode;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.IAccConstruct;

public class IntroOpenACCLoopCheck extends ForLoopCheck<RefactoringParams> {

    private final boolean kernels;
    private final boolean inParallelRegion;
    private final boolean inKernelsRegion;

    public IntroOpenACCLoopCheck(RefactoringStatus status, final IASTForStatement loop, boolean kernels) {
        super(status, loop);
        this.kernels = kernels;
        this.inParallelRegion = ancestorHasPragma(loop, ASTAccParallelNode.class, ASTAccParallelLoopNode.class);
        this.inKernelsRegion = ancestorHasPragma(loop, ASTAccKernelsNode.class, ASTAccKernelsLoopNode.class);
    }

    @SafeVarargs
    private static boolean ancestorHasPragma(IASTForStatement loop, Class<? extends IAccConstruct>... classes) {
        for (IASTNode node = loop.getParent(); node != null; node = node.getParent()) {
            if (node instanceof IASTStatement) {
                IASTStatement stat = (IASTStatement) node;
                for (Class<? extends IAccConstruct> clazz : classes) {
                    if (OpenACCUtil.isAccConstruct(stat, clazz)) {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isInParallelRegion() {
        return inParallelRegion;
    }

    public boolean isInKernelsRegion() {
        return inKernelsRegion;
    }

    @Override
    protected void doLoopFormCheck() {
        if (kernels && inParallelRegion) {
            status.addFatalError(Messages.IntroOpenACCLoopCheck_KernelsCannotInParallelRegion);
        } else if (!kernels && inKernelsRegion) {
            status.addFatalError(Messages.IntroOpenACCLoopCheck_ParallelCannotInKernelsRegion);
        }
        checkForExistingPragma();
    }

    @Override
    public void doDependenceCheck(DependenceAnalysis dep) {
        if (dep != null && dep.hasLevel1CarriedDependence()) {
            status.addError(Messages.IntroOpenACCLoopCheck_CannotParallelizeCarriesDependence);
            for (DataDependence d : dep.getDependences()) {
                if (d.isLoopCarried()) {
                	//status.addError("    " + d.toStringForErrorMessage(), createStatusContextForDependence(d));
                	//breaks cli
                }
            }
        }
    }

    private void checkForExistingPragma() {
        if (!ASTUtil.getPragmaNodes(loop).isEmpty()) {
            status.addFatalError(Messages.IntroOpenACCLoopCheck_PragmaCannotBeAddedHasPragma);
        }
    }
}
