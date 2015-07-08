package edu.auburn.oaccrefac.core.change;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

public class InterchangeLoops extends ForLoopChange {

    private IASTForStatement m_second;

    public InterchangeLoops(ASTRewrite rewriter,
            IASTForStatement first, IASTForStatement second) {
        super(rewriter, first);
        if (second != null) {
            m_second = second;
        } else {
            throw new IllegalArgumentException("Target loop cannot be null!");
        }
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(this.getLoopToChange());
        if (!inq.isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be interchanged.");
            return init;
        }
        
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        if (!headers.contains(m_second)) {
            init.addFatalError("Second loop is not within headers of first");
            return init;
        }
        
        /*
         * TODO and FIXME
         * We need to put this in a seperate algorithm for reusability...
         * 
        int numEnclosingLoops = countEnclosingLoops(getOriginal());
        DependenceAnalysis dependenceAnalysis = performDependenceAnalysis(
                init, getProgressMonitor(), getOriginal());
        boolean isValid = isInterchangeValid(numEnclosingLoops, m_depth + numEnclosingLoops,
                dependenceAnalysis.getDependences());
        if (dependenceAnalysis != null && !isValid) {
            init.addError("Interchanging the selected loop with the loop at "
                        + "depth " + m_depth + " will change the dependence structure "
                        + "of the loop nest.");
        }
        */
        
        return init;
    }
    
    /*
    private int countEnclosingLoops(IASTNode outsideOf) {
        int result = 0;
        for (IASTNode node = outsideOf.getParent(); node != null; node = node.getParent()) {
            if (node instanceof IASTForStatement) {
                result++;
            }
        }
        return result;
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
    */

    @Override
    public ASTRewrite doChange(ASTRewrite rewriter) {
        IASTForStatement first = getLoopToChange();
        return this.exchangeLoopHeaders(rewriter, first, m_second);
    }

    
    
}
