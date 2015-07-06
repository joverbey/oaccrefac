package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;

import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

/**
 * This class defines the base strategy interface to be derived
 * from for changes made to a for loop.
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopChange extends Change<IASTForStatement> {
    
    public ForLoopChange(IASTForStatement loop) {
        super(loop);
    }
    
}
