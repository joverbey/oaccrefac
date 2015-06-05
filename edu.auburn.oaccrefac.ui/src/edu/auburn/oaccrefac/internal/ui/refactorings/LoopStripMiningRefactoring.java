package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;

public class LoopStripMiningRefactoring extends ForLoopRefactoring {

    private int m_stripFactor;
    
    public LoopStripMiningRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_stripFactor = 1;
    }
    
    public boolean setStripFactor(int factor) {
        if (factor > 1) {
            m_stripFactor = factor;
            return true;
        }
        return false;
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        // TODO Auto-generated method stub

    }

}
