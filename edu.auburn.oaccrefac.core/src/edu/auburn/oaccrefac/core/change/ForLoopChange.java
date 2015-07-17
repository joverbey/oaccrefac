package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

/**
 * This class defines the base strategy interface to be derived
 * from for changes made to a for loop.
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopChange extends ASTChange {
    
    private IASTForStatement m_loop;
    public ForLoopChange(IASTRewrite rewriter, IASTForStatement loopToChange) {
        super(rewriter);
        if (loopToChange == null) {
            throw new IllegalArgumentException("Argument loop cannot be null!");
        }
        m_loop = loopToChange;
    }
    
    public IASTForStatement getLoopToChange() {
        return m_loop;
    }
    
    public void setLoopToChange(IASTForStatement newLoop) {
        if (newLoop == null) {
            throw new IllegalArgumentException("Argument loop cannot be null!");
        }
        m_loop = newLoop;
    }
    
    protected IASTRewrite exchangeLoopHeaders(IASTRewrite rewriter,
            IASTForStatement loop1, IASTForStatement loop2) {
        this.removePragmas(loop1);
        this.removePragmas(loop2);
        this.safeReplace(rewriter,
                loop1.getInitializerStatement(), 
                loop2.getInitializerStatement());
        this.safeReplace(rewriter,
                loop1.getConditionExpression(), 
                loop2.getConditionExpression());
        this.safeReplace(rewriter,
                loop1.getIterationExpression(), 
                loop2.getIterationExpression());
        this.safeReplace(rewriter,
                loop2.getInitializerStatement(), 
                loop1.getInitializerStatement());
        this.safeReplace(rewriter,
                loop2.getConditionExpression(), 
                loop1.getConditionExpression());
        this.safeReplace(rewriter,
                loop2.getIterationExpression(), 
                loop1.getIterationExpression());
        this.insertPragmas(loop1, this.getPragmas(loop2));
        this.insertPragmas(loop2, this.getPragmas(loop1));
        this.finalizePragmas(m_loop.getTranslationUnit());
        return rewriter;
    }
    
}
