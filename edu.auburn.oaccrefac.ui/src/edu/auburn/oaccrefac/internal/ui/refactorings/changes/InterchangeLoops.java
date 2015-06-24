package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

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
