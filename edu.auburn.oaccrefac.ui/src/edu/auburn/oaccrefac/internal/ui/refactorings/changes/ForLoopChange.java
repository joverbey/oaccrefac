package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

/**
 * This class defines the base strategy interface to be derived
 * from for changes made to a for loop.
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopChange {
    
    private IASTForStatement m_loop;
    
    public ForLoopChange(IASTForStatement loop) {
        if (loop != null) {
            m_loop = loop;
        } else {
            throw new IllegalStateException("Cannot change a null object");
        }
    }
    
    /**
     * Abstract method describes the implementation that all changes must
     * define. This method takes in a loop and changes it in respect to
     * it's intended purpose.
     * @param loop -- the loop in which to change
     * @return reference to changed loop
     */
    protected abstract IASTForStatement doChange(IASTForStatement loop);
    
    /**
     * This method is the runner for all changes.
     * 
     * @return reference to the changed loop
     */
    public final IASTForStatement change() {
        if (m_loop != null) {
            return doChange(m_loop.copy());
        } else {
            return null;
        }
    }
    
    public IASTForStatement getOriginal() { return m_loop; }
    
}
