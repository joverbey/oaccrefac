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
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;

public class IntroOpenACCLoopCheck extends ForLoopCheck<RefactoringParams> {

    private final boolean kernels;
    private final boolean inParallelRegion;
    private final boolean inKernelsRegion;

    public IntroOpenACCLoopCheck(final IASTForStatement loop, boolean kernels) {
        super(loop);
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
    protected void doLoopFormCheck(RefactoringStatus status) {
        if (kernels && inParallelRegion) {
            status.addFatalError("A kernels loop cannot be introduced in a parallel region.");
        } else if (!kernels && inKernelsRegion) {
            status.addFatalError("A parallel loop cannot be introduced in a kernels region.");
        }
        checkForExistingPragma(status);
    }

    @Override
    public void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        if (dep != null && dep.hasLevel1CarriedDependence()) {
            status.addError("This loop cannot be parallelized because it carries a dependence.");
        }
    }

    private void checkForExistingPragma(RefactoringStatus status) {
        if (!ASTUtil.getPragmaNodes(loop).isEmpty()) {
            status.addFatalError("A pragma cannot be added to a loop that already has a pragma on it.");
        }
    }
}
