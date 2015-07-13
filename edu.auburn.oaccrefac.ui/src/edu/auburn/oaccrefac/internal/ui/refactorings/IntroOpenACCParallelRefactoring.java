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
 ******************************************************************************/
package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

/**
 * Refactoring that adds a <code>#pragma acc parallel</code> directive to a for-loop.
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
@SuppressWarnings("restriction")
public class IntroOpenACCParallelRefactoring extends ForLoopRefactoring {

    public IntroOpenACCParallelRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        // TODO: Check for existing/conflicting OpenACC pragma

        DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(status, pm, getLoop());
        if (dependenceAnalysis != null && dependenceAnalysis.hasLevel1CarriedDependence()) {
            status.addError("This loop cannot be parallelized because it carries a dependence.",
                    getLocation(getLoop()));
        }
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        IASTNode pragma = rewriter.createLiteralNode("#pragma acc parallel loop\n");
        rewriter.insertBefore(getLoop().getParent(), getLoop(), pragma, new TextEditGroup("Insert #pragma"));
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null; // Refactoring history is not supported.
    }
}
