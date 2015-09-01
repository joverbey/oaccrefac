package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.transformations.SourceAlteration;
import edu.auburn.oaccrefac.core.transformations.StripMineAlteration;
import edu.auburn.oaccrefac.core.transformations.StripMineCheck;
import edu.auburn.oaccrefac.core.transformations.IASTRewrite;

public class LoopStripMiningRefactoring extends ForLoopRefactoring {

    private int stripFactor;
    private int depth;
    private StripMineCheck check;
    
    public LoopStripMiningRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        this.stripFactor = -1;
    }
    
    public boolean setStripFactor(int factor) {
        this.stripFactor = factor;
        return true;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        check = new StripMineCheck(getLoop());
        check.performChecks(status, pm, null);
    }

    @Override
    protected void refactor(IASTRewrite rewriter, IProgressMonitor pm) {
        new StripMineAlteration(getAST(), rewriter, getLoop(), stripFactor, depth, check).change();
    }
    
}
