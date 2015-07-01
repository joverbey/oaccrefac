package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.ui.refactorings.changes.Change;
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.TileLoops;

/**
 * "The basic algorithm for blocking (tiling) is called strip-mine-and-interchange.
 * Basically, it consists of strip-mining a given loop into two loops, one that
 * iterates within contiguous strips and an outer loop that iterates strip-by-strip,
 * then interchanging the by-strip loop to the outside of the outer containing loops."
 * -- 9.3.2 Legality of Blocking, p.480, Optimizing Compilers for Modern Architectures
 * 
 */
public class LoopTilingRefactoring extends ForLoopRefactoring {

    private int m_depth;
    private int m_stripFactor;
    private int m_propagate;
    
    private Change<?> m_tileChange;
    
    public LoopTilingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_depth = 0;
        m_propagate = -1;
        m_stripFactor = 0;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        m_tileChange = new TileLoops(getLoop(), 
                m_depth, m_stripFactor, m_propagate);
        m_tileChange.checkConditions(status, pm);
    }
    
    public void setStripMineDepth(int depth) {
        m_depth = depth;
    }
    
    public void setStripFactor(int factor) {
        m_stripFactor = factor;
    }
    
    public void setPropagateInterchange(int prop) {
        m_propagate = prop;
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        rewriter = m_tileChange.change(rewriter);
//        rewriter.replace(getLoop(), refactored, null);
    }
}
