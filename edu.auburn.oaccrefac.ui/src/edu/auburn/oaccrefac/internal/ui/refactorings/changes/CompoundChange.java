package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import java.util.LinkedList;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

/**
* This class defines the base strategy interface to be derived
* from for changes made to a for loop.
* @author Adam Eichelkraut
*
*/
public abstract class CompoundChange {
   
   private IASTCompoundStatement m_compound;
   private LinkedList<IASTStatement> m_chilluns;
   
   public CompoundChange(IASTCompoundStatement compoundCopy) {
       if (!m_compound.isFrozen()) {
           m_compound = compoundCopy;
           m_chilluns = new LinkedList<IASTStatement>();
           for (IASTStatement child : m_compound.getStatements())
               m_chilluns.add(child);
       } else {
           throw new IllegalStateException("Error attempting"
                   + "to make a CompoundChange object with"
                   + "a frozen node. Use node.copy() to create"
                   + "an unfrozen copy of the node first.");
       }
   }
   
   /**
    * Abstract method describes the implementation that all changes must
    * define. This method takes in a loop and changes it in respect to
    * it's intended purpose.
    * @param loop -- the loop in which to change
    * @return reference to changed loop
    */
   protected abstract IASTCompoundStatement doChange(IASTCompoundStatement loop);
   
   /**
    * This method is the runner for all changes.
    * 
    * @return reference to the changed loop
    */
   public final IASTCompoundStatement change() {
       if (m_compound != null) {
           return doChange(m_compound);
       } else {
           return null;
       }
   }
   
   protected final LinkedList<IASTStatement> getStatementList() {
       return m_chilluns;
   }
   
}