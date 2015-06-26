package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;

/**
* This class defines the base strategy interface to be derived
* from for changes made to a for loop.
* @author Adam Eichelkraut
*
*/
public abstract class CompoundChange extends Change<IASTCompoundStatement> {
   
   public CompoundChange(IASTCompoundStatement compoundStatement) {
       super(compoundStatement);
   }
   
}