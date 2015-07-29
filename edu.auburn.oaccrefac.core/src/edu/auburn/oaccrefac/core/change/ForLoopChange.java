package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

/**
 * This class defines the base strategy interface to be derived
 * from for changes made to a for loop.
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopChange extends ASTChange {
    
    //Members
    private IASTForStatement m_loop;
    
    /**
     * Constructor that takes a for-loop and a rewriter (for base)
     * @author Adam Eichelkraut
     * @param rewriter -- rewriter to be given to base class
     * @param loopToChange -- loop to change
     * @throws IllegalArgumentException if the for loop is null
     */
    public ForLoopChange(IASTRewrite rewriter, IASTForStatement loopToChange) {
        super(rewriter);
        if (loopToChange == null) {
            throw new IllegalArgumentException("Argument loop cannot be null!");
        }
        m_loop = loopToChange;
    }
    
    /**
     * Gets the loop set from constructor
     * @author Adam Eichelkraut
     * @return loop to change
     */
    public IASTForStatement getLoopToChange() {
        return m_loop;
    }
    
    /**
     * Sets the loop to change
     * @author Adam Eichelkraut
     * @param newLoop, not null
     * @throws IllegalArgumentException if argument is null
     */
    public void setLoopToChange(IASTForStatement newLoop) {
        if (newLoop == null) {
            throw new IllegalArgumentException("Argument loop cannot be null!");
        }
        m_loop = newLoop;
    }
    
    /**
     * Exchanges two loop headers, not sure why this isn't in an
     * inherited class. But for now it replaces the headers from two
     * loops. It also handles swapping pragmas.
     * @param rewriter -- rewriter to do swaps with
     * @param loop1 -- first loop to swap headers
     * @param loop2 -- second loop to swap headers
     * @return -- returns rewriter used
     */
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
