package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import java.util.LinkedList;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;

/**
* This class defines the base strategy interface to be derived
* from for changes made to a for loop.
* @author Adam Eichelkraut
*
*/
public abstract class CompoundChange {
   
   private IASTCompoundStatement m_compound;
   private LinkedList<IASTStatement> m_chilluns;
   
   public CompoundChange(IASTCompoundStatement compoundStatement) {
       m_compound = compoundStatement;
       m_chilluns = new LinkedList<IASTStatement>();
       for (IASTStatement child : m_compound.getStatements())
           m_chilluns.add(child);
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
   
   protected IASTCompoundStatement rebuildCompound() {
       ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
       IASTCompoundStatement newCompound = factory.newCompoundStatement();
       for (IASTStatement child : m_chilluns) {
           newCompound.addStatement(child.copy());
       }
       return newCompound;
   }
   
   protected final LinkedList<IASTStatement> getStatementList() {
       return m_chilluns;
   }
   
}