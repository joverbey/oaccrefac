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
 *     Carl Worley (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.FileStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ptp.pldt.openacc.core.dependence.DataDependence;
//import org.eclipse.ptp.pldt.openacc.core.dependence.DataDependence;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceAnalysis;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceTestFailure;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class ForLoopCheck<T extends RefactoringParams> extends Check<T> {

    protected final IASTForStatement loop;
    
    protected ForLoopCheck(RefactoringStatus status, IASTForStatement loop) {
    	super(status);
        this.loop = loop;
    }
    
    protected void doLoopFormCheck() { }
    
    protected void doDependenceCheck(DependenceAnalysis dep) { }

    public RefactoringStatus loopFormCheck(IProgressMonitor pm) {
    	if (ASTUtil.getUnsupportedOp(loop.getBody()) != null) {
            status.addError(
                    Messages.ForLoopCheck_CannotRefactor);
        }
    	doLoopFormCheck();
        return status;
    }
    
    public RefactoringStatus dependenceCheck(IProgressMonitor pm) {
        
        IASTStatement[] statements;
        DependenceAnalysis dependenceAnalysis;
        
        statements = ASTUtil.getStatementsIfCompound(loop.getBody());
        
        try {
            dependenceAnalysis = new DependenceAnalysis(pm, statements);
        } catch (DependenceTestFailure e) {
            status.addError(Messages.ForLoopCheck_CannotAnalyzeDependences + e.getMessage());
            return status;
        }
        
        doDependenceCheck(dependenceAnalysis);
        return status;
    }
    
    @Override
    public RefactoringStatus performChecks(IProgressMonitor pm, T params) {
        super.performChecks(pm, params);
        if(status.hasFatalError()) {
            return status;
        }
        loopFormCheck(pm);
        if(status.hasFatalError()) {
            return status;
        }
        dependenceCheck(pm);
        return status;
    }

    public IASTForStatement getLoop() {
        return loop;
    }
    
    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return loop.getTranslationUnit();
    }
    
    protected RefactoringStatusContext createStatusContextForDependence(DataDependence d) {
        return ASTUtil.getStatusContext(d.getAccess1().getVariableName(), d.getAccess2().getVariableName());
    }

    protected RefactoringStatusContext getLocation(IASTNode node1, IASTNode node2) {
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
}
