package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.ui.refactorings.changes.StripMine;

public class LoopStripMiningRefactoring extends ForLoopRefactoring {

    private int m_stripFactor;
    private StripMine change;
    
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
        change = new StripMine(getLoop(), m_stripFactor, 0);
        change.setProgressMonitor(pm);
        change.checkConditions(status);
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {        
        rewriter.replace(getLoop(), change.change(), null);
    }
    
}
