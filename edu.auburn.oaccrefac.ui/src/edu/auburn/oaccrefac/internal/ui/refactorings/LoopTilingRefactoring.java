/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.TileLoopsAlteration;
import edu.auburn.oaccrefac.core.transformations.TileLoopsCheck;
import edu.auburn.oaccrefac.core.transformations.TileLoopsParams;

/**
 * "The basic algorithm for blocking (tiling) is called strip-mine-and-interchange.
 * Basically, it consists of strip-mining a given loop into two loops, one that
 * iterates within contiguous strips and an outer loop that iterates strip-by-strip,
 * then interchanging the by-strip loop to the outside of the outer containing loops."
 * -- 9.3.2 Legality of Blocking, p.480, Optimizing Compilers for Modern Architectures
 * 
 */
public class LoopTilingRefactoring extends ForLoopRefactoring {

    private int width;
    private int height;
    private TileLoopsCheck check;
    
    public LoopTilingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        this.width = 0;
        this.height = 0;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new TileLoopsCheck(getLoop());
        check.performChecks(status, pm, new TileLoopsParams(width, height));
    }

    /** FIXME
     * Ideally, we could repeatedly create and use StripMineChecks, StripMineAlterations, InterchangeChecks, and
     * InterchangeAlterations, passing the editions of each alteration on to the next one before performing it
     * However, we may need to do strip mining, then interchange dep analysis on the result, then interchange. 
     * So: is it true that if an interchange is valid, it will be valid after strip mining has occurred, 
     * and is it true that if an interchange is invalid, it will be invalid after strip mining has occurred?
     * 
     * What I'm thinking right now is that we can do all of the strip mining first without affecting dependence structure at all, 
     * then when we do interchange (note, on loops that don't exist yet) on two loops in the new nest, 
     * we can check whether the interchange is valid for the loops that the loops being interchanged came from. 
     * I.e., treat either of the loops that resulted from strip mining i as the i loop, either of the ones from strip
     * mining j as the j loop, etc, and the do the dependence checks on the original i and j loops. 
     */
    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException {
        new TileLoopsAlteration(rewriter, width, height, check).change();
    }
}
