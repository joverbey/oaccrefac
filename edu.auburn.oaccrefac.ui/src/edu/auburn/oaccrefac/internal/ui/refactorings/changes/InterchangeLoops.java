package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.OperationCanceledException;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class InterchangeLoops extends ForLoopChange {

    int m_depth;
    
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
        if (m_depth < 0) {
            throw new IllegalArgumentException("Depth cannot be less than 0.");
        }
    }

    @Override
    public IASTForStatement doChange(IASTForStatement loop) {
        
        IASTForStatement temp = getOriginal().copy();
        IASTForStatement xchng = loop;
        xchng = ASTUtil.findDepth(xchng, IASTForStatement.class, m_depth);
        if (xchng == null) {
            throw new OperationCanceledException(
                    "Could not find loop at depth: " + m_depth);
        }
        
        loop.setInitializerStatement(xchng.getInitializerStatement());
        loop.setConditionExpression(xchng.getConditionExpression());
        loop.setIterationExpression(xchng.getIterationExpression());
        
        xchng.setInitializerStatement(temp.getInitializerStatement());
        xchng.setConditionExpression(temp.getConditionExpression());
        xchng.setIterationExpression(temp.getIterationExpression());

        return loop;
    }

}
