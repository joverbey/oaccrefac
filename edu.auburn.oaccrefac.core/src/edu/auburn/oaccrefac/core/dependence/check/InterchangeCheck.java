package edu.auburn.oaccrefac.core.dependence.check;

import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dependence.DataDependence;
import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.Direction;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

public class InterchangeCheck extends DependenceCheck {

    private ForStatementInquisitor m_inq;
    private IASTForStatement m_base;
    private IASTForStatement m_second;
    
    
    public InterchangeCheck(IASTForStatement base, IASTForStatement second) {
        super(base);
        
        m_base = base;
        m_second = second;
        
        if (m_base == null || m_second == null) {
            throw new IllegalArgumentException("Inputs cannot be null!");
        }
        
        m_inq = InquisitorFactory.getInquisitor(m_base);
    }
    
    @Override
    public RefactoringStatus doCheck(RefactoringStatus status, DependenceAnalysis dep) {
        
        List<IASTForStatement> headers = m_inq.getPerfectLoopNestHeaders();
        int second_depth = headers.indexOf(m_second);
        if (second_depth > 0) {
            int numEnclosingLoops = countEnclosingLoops(m_base);
            boolean isValid = isInterchangeValid(numEnclosingLoops, 
                    second_depth + numEnclosingLoops,
                    dep.getDependences());
            if (!isValid) {
                status.addError("Interchanging the selected loop with the loop at "
                            + "depth " + second_depth + " will change the dependence structure "
                            + "of the loop nest.");
            }
        } else {
            throw new IllegalArgumentException("Second for-statement must be within"
                    + "perfectly nested loop headers of first.");
        }
        return null;
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
    


}
