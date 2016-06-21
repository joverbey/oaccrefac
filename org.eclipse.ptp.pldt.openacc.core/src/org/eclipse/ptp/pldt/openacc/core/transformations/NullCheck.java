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
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ConstantPropagation;
import org.eclipse.ptp.pldt.openacc.core.dependence.DataDependence;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class NullCheck extends ForLoopCheck<NullParams> {

    public NullCheck(IASTForStatement loop) {
        super(loop);
        IASTFunctionDefinition enclosing = ASTUtil.findNearestAncestor(loop, IASTFunctionDefinition.class);
        new ConstantPropagation(enclosing);
    }

    @Override
    protected void doLoopFormCheck(RefactoringStatus status) {
        ForStatementInquisitor inquisitor = ForStatementInquisitor.getInquisitor(loop);

        // If the loop is not a counted loop, fail
        if (!inquisitor.isCountedLoop()) {
            status.addFatalError("Loop form not supported (not a 0-based counted loop).");
            return;
        }

        Long ub = inquisitor.getInclusiveUpperBound();
        if (ub != null) {
            status.addInfo(String.format("Loop upper bound is %d.", ub));
        }

        if (inquisitor.isPerfectLoopNest()) {
            status.addInfo("Loop is a perfect loop nest.");
        }

        // If the loop contains unsupported statements, fail
        IASTNode unsupported = inquisitor.getFirstUnsupportedStmt();
        if (unsupported != null) {
            status.addFatalError("Loop contains unsupported statement: " + ASTUtil.toString(unsupported).trim());
        }
    }

    @Override
    public void doDependenceCheck(RefactoringStatus status, DependenceAnalysis dep) {
        if (dep == null) {
            status.addFatalError("Dependences could not be analyzed");
            return;
        }

        List<DataDependence> deps = new ArrayList<DataDependence>(dep.getDependences());
        deps.sort(new Comparator<DataDependence>() {
			@Override
			public int compare(DataDependence o1, DataDependence o2) {
				return o1.toString().compareTo(o2.toString());
			}
        });
        for (DataDependence d : deps) {
            status.addInfo(d.toString(), createStatusContextForDependence(d));
        }
    }

    @Override
    protected void doParameterCheck(RefactoringStatus status, NullParams params) {
    }
}