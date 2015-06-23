package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.StripMine;

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
    
    public LoopTilingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_depth = 0;
        m_propagate = -1;
        m_stripFactor = 0;
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        if (!ForStatementInquisitor.getInquisitor(getLoop()).isPerfectLoopNest()) {
            status.addFatalError("Only perfectly nested loops can be interchanged.");
        }
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        @SuppressWarnings("unused")
        DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(status, pm);
        // if (dependenceAnalysis != null && dependenceAnalysis.()) {
        // status.addError("This loop cannot be parallelized because it carries a dependence.",
        // getLocation(getLoop()));
        // }
        
        //Check to make sure the depth is valid
        if (m_depth < 1) {
            status.addFatalError("Strip mine depth cannot be less than 1 (must occur to"
                    + "a nested loop to tile.)");
        }
        if (m_stripFactor < 1) {
            status.addFatalError("Strip factor must be greater than 1 for refactoring to"
                    + "be of any use. Exiting...");
        }
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
        StripMine sm = new StripMine(getLoop(), m_stripFactor, m_depth);
        IASTForStatement refactored = sm.change();
//        for (int i = 0; (i < m_propagate && m_depth-i > 0); i++) {
//            IASTForStatement toInterchange = ASTUtil.findDepth(refactored, IASTForStatement.class, m_depth-i-1);
//            InterchangeLoops il = new InterchangeLoops(toInterchange, m_depth-i);
//            refactored = il.change();
//        }
        rewriter.replace(getLoop(), refactored, null);
    }

}
