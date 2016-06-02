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

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.FileStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
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

        for (DataDependence d : dep.getDependences()) {
            status.addInfo(d.toString(), getLocation(d.getAccess1().getVariableName(), d.getAccess2().getVariableName()));
        }
    }

    private RefactoringStatusContext getLocation(IASTNode node1, IASTNode node2) {
        if (node1.getTranslationUnit() != node2.getTranslationUnit()) {
            return null;
        }
        
        String filename = node1.getTranslationUnit().getFileLocation().getFileName();
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(filename));
        if (file == null) {
            return null;
        }

        IASTFileLocation fileLocation1 = node1.getFileLocation();
        IASTFileLocation fileLocation2 = node2.getFileLocation();
        int start1 = fileLocation1.getNodeOffset();
        int end1 = start1 + fileLocation1.getNodeLength();
        int start2 = fileLocation2.getNodeOffset();
        int end2 = start2 + fileLocation2.getNodeLength();
        int start = Math.min(start1, start2);
        int end = Math.max(end1, end2);
        return new FileStatusContext(file, new Region(start, end - start));
    }

    @Override
    protected void doParameterCheck(RefactoringStatus status, NullParams params) {
    }
}