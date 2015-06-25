package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

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

        loop.getTranslationUnit().getAllPreprocessorStatements()[0].getFileLocation();
        
        return loop;
    }

    private List<IASTPreprocessorPragmaStatement> getLeadingPragmas(IASTForStatement loop) {
        int loopLoc = loop.getFileLocation().getNodeOffset();
        int precedingStmtOffset = getNearestPrecedingStatementOffset(loop);
        List<IASTPreprocessorPragmaStatement> pragmas = new ArrayList<IASTPreprocessorPragmaStatement>();
        for(IASTPreprocessorStatement pre : loop.getTranslationUnit().getAllPreprocessorStatements()) {
            if(pre instanceof IASTPreprocessorPragmaStatement &&
                    ((IASTPreprocessorPragmaStatement) pre).getFileLocation().getNodeOffset() < loopLoc &&
                    ((IASTPreprocessorPragmaStatement) pre).getFileLocation().getNodeOffset() > precedingStmtOffset) {
                pragmas.add((IASTPreprocessorPragmaStatement) pre);
            }
        }
        return pragmas;
    }
    
    private int getNearestPrecedingStatementOffset(IASTStatement stmt) {
        
        class OffsetFinder extends ASTVisitor {
            
            //the offset of the nearest lexical predecessor of the given node
            int finalOffset;
            int thisOffset;
            
            public OffsetFinder(int offset) {
                shouldVisitStatements = true;
                this.thisOffset = offset;
            }

            @Override
            public int visit(IASTStatement stmt) {
                int foundOffset = stmt.getFileLocation().getNodeOffset();
                if(thisOffset - foundOffset < finalOffset && foundOffset < thisOffset) {
                    this.finalOffset = foundOffset;
                }
                return PROCESS_CONTINUE;
            }
            
        }
        
        OffsetFinder finder = new OffsetFinder(stmt.getFileLocation().getNodeOffset());
        IASTFunctionDefinition containingFunc = ASTUtil.findNearestAncestor(stmt, IASTFunctionDefinition.class);
        containingFunc.accept(finder);
        return finder.finalOffset;
    }
    
}
