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
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.FizzLoops;

/**
 * This class defines the implementation for re-factoring using loop fusion. For example:
 * 
 * ORIGINAL:                  REFACTORED: 
 * int i, a[100], b[100];     | int i, a[100], b[100]; 
 * for (i = 0; i < 100; i++) {| for (i = 0; i <100; i++)
 *    a[i] = 1;               |     a[i] = 1;
 *    b[i] = 2;               | for (i = 0; i <100; i++)
 * }                          |     b[i] = 2; 
 *                            |
 * 
 * (Example taken from Wikipedia's web page on loop Fission.)
 */
public class LoopFissionRefactoring extends ForLoopRefactoring {

    //I've always wanted to use a question mark like this.
    private Change<?> m_fizzChange;
    
    public LoopFissionRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        // This gets the selected loop to re-factor and checks if the body is compound statement only..
        IASTForStatement loop = getLoop();
        IASTCompoundStatement enclosingCompound = 
                ASTUtil.findNearestAncestor(loop, IASTCompoundStatement.class);
        m_fizzChange = new FizzLoops(enclosingCompound, loop);
        m_fizzChange.setProgressMonitor(pm);
        m_fizzChange.checkConditions(initStatus);
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        rewriter.replace(m_fizzChange.getOriginal(), m_fizzChange.change(), null);
    }

}
