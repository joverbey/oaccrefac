package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class FizzLoops extends CompoundModify {
    
    private IASTForStatement m_loop;
    
    public FizzLoops(IASTCompoundStatement compound, IASTForStatement loop) {
        super(compound);
        m_loop = loop;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        //If the loop doesn't have children, bail. Save some
        //energy by not doing the refactoring.
        if (!(m_loop.getBody() instanceof IASTCompoundStatement)) {
            init.addFatalError("Body does not have any statements"
                    + " therefore, loop fission is useless.");
            return init;
        }
        
        if (m_loop.getBody().getChildren().length < 2) {
            init.addWarning("Warning: Loop fission refactoring is "
                    + "useless with less than two statements in body.");
        }
        
        //If the loop is frozen, we unfreeze, replace, and work with replacement
        if (m_loop.isFrozen()) {
            IASTForStatement copy = m_loop.copy();
            this.replace(m_loop, copy);
            m_loop = copy;
        }
        
        return init;
    }
    
    @Override
    protected void modifyCompound() {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        
        //Ensured from precondition...
        IASTCompoundStatement body = (IASTCompoundStatement) m_loop.getBody();
        
        /*
         * Over here, we are looking for all the statements in the body of for-loop
         * which can be put into individual for-loop of their own.
         */
        for (IASTStatement child : body.getStatements()) {
            IASTForStatement newForLoop = factory.newForStatement
                    (m_loop.getInitializerStatement(), 
                            m_loop.getConditionExpression(), 
                            m_loop.getIterationExpression(), 
                            child);
            EnsureCompoundBody check = new EnsureCompoundBody(newForLoop);
            this.insertAfter(newForLoop, check.change(this));
        }
        
        //Remove the old loop from the statement list
        if (body.getStatements().length > 0) {
            this.remove(m_loop);
        }
    }

}
