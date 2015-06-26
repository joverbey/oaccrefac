package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.Change;
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.FuseLoops;

/**
 * This class defines the implementation for re-factoring using loop fusion. For example:
 * 
 * ORIGINAL:                  REFACTORED: 
 * int i, a[100], b[100];    | int i, a[100], b[100]; 
 * for (i = 0; i < 100; i++) | for (i = 0; i <100; i++)
 *    a[i] = 1;              | { 
 * for (i = 0; i < 100; i++) | a[i] = 1; 
 *    b[i] = 2;              | b[i] = 2; 
 *                           | }
 * 
 * (Example taken from Wikipedia's web page on loop Fusion)
 */
public class LoopFusionRefactoring extends ForLoopRefactoring {

    private Change<?> m_fuseLoops;

    public LoopFusionRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        IASTForStatement loop = getLoop();
        IASTCompoundStatement enclosingCompound = 
                ASTUtil.findNearestAncestor(loop, IASTCompoundStatement.class);
        m_fuseLoops = new FuseLoops(enclosingCompound, loop);
        m_fuseLoops.setProgressMonitor(pm);
        m_fuseLoops.checkConditions(initStatus);
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        rewriter.replace(m_fuseLoops.getOriginal(), m_fuseLoops.change(), null);
    }

}
