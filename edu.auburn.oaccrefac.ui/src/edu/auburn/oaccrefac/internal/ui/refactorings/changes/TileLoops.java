package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

public class TileLoops extends ForLoopChange {

    private int m_depth;
    private int m_stripFactor;
    private int m_propagate;
    
    public TileLoops(IASTForStatement loop, 
            int stripDepth, int stripFactor, int propagateInterchange) {
        super(loop);
        m_depth = stripDepth;
        m_stripFactor = stripFactor;
        m_propagate = propagateInterchange;
    }

    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        //DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(status, pm);
        // if (dependenceAnalysis != null && dependenceAnalysis.()) {
        // status.addError("This loop cannot be parallelized because it carries a dependence.",
        // getLocation(getLoop()));
        // }
        
        if (!ForStatementInquisitor.getInquisitor(getOriginal()).isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be tiled.");
            return init;
        }
        
        if (m_propagate > m_depth) {
            init.addWarning("Warning: propagation higher than depth -- propagation "
                    + "will occur as many times as possible.");
            return init;
        }
        
        Change<?> checkStripMine = new StripMine(getOriginal(), m_stripFactor, m_depth);
        checkStripMine.setProgressMonitor(getProgressMonitor());
        if (checkStripMine.checkConditions(init).hasFatalError()) {
            return init;
        }
        
        return init;
    }
    
    @Override
    protected IASTForStatement doChange(IASTForStatement nodeToChange) {
        StripMine sm = new StripMine(nodeToChange, m_stripFactor, m_depth);
        nodeToChange = sm.change(this);
        for (int i = 0; checkIteration(i); i++) {
            InterchangeLoops il = new InterchangeLoops(nodeToChange, m_depth-i, m_depth-i-1);
            nodeToChange = il.change(this);
        }
        return nodeToChange;
    }
    
    private boolean checkIteration(int iteration) {
        if (m_propagate < 0) {
            return (m_depth-iteration > 0);
        } else {
            return (iteration < m_propagate && m_depth-iteration > 0);
        }
    }

}
