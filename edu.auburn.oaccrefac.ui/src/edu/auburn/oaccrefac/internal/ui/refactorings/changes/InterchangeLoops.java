package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DataDependence;
import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.Direction;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;

public class InterchangeLoops extends ForLoopChange {

    private int m_exchange;
    private int m_depth;
    
    /**
     * for (int i = 0 ...)   <--- depth 0
     *      for (int j = 0 ...)  <--- depth 1
     *          for (int k = 0 ...)  <--- depth 2
     * @param first
     * @param depth
     */
    public InterchangeLoops(IASTForStatement first, int depth) {
        super(first);
        m_depth = depth;
        m_exchange = 0;
        if (m_depth < 0) {
            throw new IllegalArgumentException("Depth cannot be less than 0.");
        }
    }
    
    public InterchangeLoops(IASTForStatement first, int depth, int toExchangeWith) {
        super(first);
        m_depth = depth;
        m_exchange = toExchangeWith;
        if (m_depth < 0) {
            throw new IllegalArgumentException("Depth cannot be less than 0.");
        }
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        ForStatementInquisitor forLoop = ForStatementInquisitor.getInquisitor(getOriginal());
        if (!forLoop.isPerfectLoopNest()) {
            init.addFatalError("Only perfectly nested loops can be interchanged.");
            return init;
        }
        
        List<IASTForStatement> headers = forLoop.getPerfectLoopNestHeaders();
        if (m_depth < 0 || m_depth >= headers.size()) {
            init.addFatalError("There is no for-loop at depth " + m_depth);
            return init;
        }
        
        if (m_exchange < 0 || m_exchange >= headers.size()) {
            init.addFatalError("There is no for-loop at exchange depth:" + m_exchange);
            return init;
        }
        
        
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
        
        return init;
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
    

    @Override
    public IASTForStatement doChange(IASTForStatement loop) {
        
        IASTForStatement left = ASTUtil.findDepth(loop, IASTForStatement.class, m_exchange);
        IASTForStatement right = ASTUtil.findDepth(loop, IASTForStatement.class, m_depth);
        IASTForStatement temp = left.copy();
        
        left.setInitializerStatement(right.getInitializerStatement());
        left.setConditionExpression(right.getConditionExpression());
        left.setIterationExpression(right.getIterationExpression());
        
        right.setInitializerStatement(temp.getInitializerStatement());
        right.setConditionExpression(temp.getConditionExpression());
        right.setIterationExpression(temp.getIterationExpression());

        return loop;
    }

}
