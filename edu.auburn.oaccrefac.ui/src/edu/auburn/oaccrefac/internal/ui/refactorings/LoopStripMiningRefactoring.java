package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.change.ASTChange;
import edu.auburn.oaccrefac.core.change.IASTRewrite;
import edu.auburn.oaccrefac.core.change.StripMine;

public class LoopStripMiningRefactoring extends ForLoopRefactoring {

    private int m_stripFactor;
    private ASTChange stripMine;
    
    public LoopStripMiningRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_stripFactor = -1;
    }
    
    public boolean setStripFactor(int factor) {
        m_stripFactor = factor;
        return true;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        stripMine = new StripMine(null, getLoop(), m_stripFactor, 0);
        stripMine.checkConditions(status, pm);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {        
        stripMine.setRewriter(rewriter);
        rewriter = stripMine.change();
        //rewriter.replace(getLoop(), change.change(), null);
    }
    
}
