package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class FizzLoops extends CompoundModify {

    private IASTForStatement m_orignalLoop;
    private IASTForStatement m_loop;
    
    public FizzLoops(IASTCompoundStatement compound, IASTForStatement loop) {
        super(compound);
        m_orignalLoop = loop;
        m_loop = loop;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        if (m_orignalLoop == null) {
            init.addFatalError("Loop argument is null!");
            return init;
        }
        // This gets the selected loop to re-factor and checks if the body is compound statement only..
        if (!(m_orignalLoop.getBody() instanceof IASTCompoundStatement)) {
            init.addFatalError("Useless to apply fission on one statement loops.");
            return init;
        } 
        
        if (m_orignalLoop.isFrozen()) {
            init.addWarning("For loop is frozen. Creating copy to modify.");
            m_loop = m_orignalLoop.copy();
        }
        return init;
    }
    
    @Override
    protected void modifyCompound() {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        
        IASTStatement body = m_loop.getBody();
        IASTNode[] chilluns = body.getChildren();
        IASTNode insert_before = ASTUtil.getNextSibling(m_orignalLoop);
        
        /*Over here, we are looking for all the statements in the body of for-loop
         *which can be put into individual for-loop of their own.
         */
        for (IASTNode child : chilluns) {
            IASTStatement stmt = (IASTStatement) (child.copy());
            IASTForStatement newForLoop = factory.newForStatement
                    (m_loop.getInitializerStatement(), 
                            m_loop.getConditionExpression(), 
                            m_loop.getIterationExpression(), 
                            stmt);
            if (insert_before != null && insert_before instanceof IASTStatement) {
                insertBefore(newForLoop, (IASTStatement) insert_before);
            } else {
                append(newForLoop);
            }
        }
        /*This check is to make sure that compound body is not empty. 
         * If yes, it'll not perform re-factoring.
         */
        if (chilluns.length>0) {
            remove(m_orignalLoop);
        }
    }

}
