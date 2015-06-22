package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DataDependence;
import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.Direction;
import edu.auburn.oaccrefac.internal.core.ForLoopUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.ui.refactorings.changes.InterchangeLoops;

/**
 * This class implements refactoring for loop interchange. Loop interchange is the exchange of the ordering of two
 * iteration variables used in nested loops.
 * 
 */
public class LoopInterchangeRefactoring extends ForLoopRefactoring {

    private int m_depth;

    public LoopInterchangeRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_depth = 1;
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
        if (!ForLoopUtil.isPerfectLoopNest(getLoop())) {
            status.addFatalError("Only perfectly nested loops can be interchanged.");
        }
    }

    public void setExchangeDepth(int depth) {
        if (depth > 0) {
            m_depth = depth;
        }
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
        ForStatementInquisitor forLoop = ForStatementInquisitor.getInquisitor(getLoop());
        List<IASTForStatement> headers = forLoop.getPerfectLoopNestHeaders();
        if (m_depth == 0) {
            status.addFatalError("A loop cannot be interchanged with itself!");
            return;
        } else if (m_depth < 0 || m_depth >= headers.size()) {
            status.addFatalError("There is no for-loop at depth " + m_depth);
            return;
        }

        int numEnclosingLoops = countEnclosingLoops(getLoop());

        DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(status, pm, getLoop());
        if (dependenceAnalysis != null && !isInterchangeValid(numEnclosingLoops, m_depth + numEnclosingLoops,
                dependenceAnalysis.getDependences())) {
            status.addError("Interchanging the selected loop with the loop at depth " + m_depth
                    + " will change the dependence structure of the loop nest.");
            return;
        }
    }

    private boolean isInterchangeValid(int i, int j, Set<DataDependence> dependences) {
        for (DataDependence dep : dependences) {
            Direction[] dirVec = dep.getDirectionVector();
            if (isValid(dirVec) && !isValid(swap(i, j, dirVec))) {
                return false;
            }
        }
        return true;
    }

    private boolean isValid(Direction[] dirVec) {
        for (Direction dir : dirVec) {
            switch (dir) {
            case EQ:
                continue;
            case LT:
            case LE:
            case ANY:
                return true;
            case GT:
            case GE:
                return false;
            default:
                throw new IllegalStateException();
            }
        }
        return true;
    }

    private Direction[] swap(int i, int j, Direction[] dirVec) {
        if (i < 0 || j < 0 || i >= dirVec.length || j >= dirVec.length) {
            throw new IllegalArgumentException();
        }
        
        Direction[] result = new Direction[dirVec.length];
        System.arraycopy(dirVec, 0, result, 0, dirVec.length);
        Direction tmp = result[i];
        result[i] = result[j];
        result[j] = tmp;
        return result;
    }

    private int countEnclosingLoops(IASTNode outsideOf) {
        int result = 0;
        for (IASTNode node = outsideOf.getParent(); node != null; node = node.getParent()) {
            if (node instanceof IASTForStatement) {
                result++;
            }
        }
        return result;
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        // Get the top level loop and the loop to refactor.
        IASTForStatement loop = getLoop();
        InterchangeLoops inter = new InterchangeLoops(loop, m_depth);
        rewriter.replace(getLoop(), inter.change(), null);
    }

}
