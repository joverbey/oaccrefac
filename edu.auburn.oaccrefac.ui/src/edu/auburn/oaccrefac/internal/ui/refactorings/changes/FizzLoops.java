package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class FizzLoops extends CompoundModify {

    private IASTForStatement m_loop;
    
    public FizzLoops(IASTCompoundStatement compound, IASTForStatement loop) {
        super(compound);
        if (loop != null) {
            m_loop = loop;
        } else {
            throw new IllegalArgumentException("Cannot fizz null loop!");
        }
    }
    
    @Override
    protected IASTCompoundStatement doChange() {
        
        IASTStatement body = m_loop.getBody();
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTNode insert_before = ASTUtil.getNextSibling(m_loop);
        IASTNode[] chilluns = body.getChildren();
        
        /*Over here, we are looking for all the statements in the body of for-loop
         *which can be put into individual for-loop of their own.
         */
        for (IASTNode child : chilluns) {
            IASTStatement stmt = (IASTStatement) (child.copy());
            IASTForStatement newForLoop = factory.newForStatement
                    (m_loop.getInitializerStatement().copy(), 
                            m_loop.getConditionExpression().copy(), 
                            m_loop.getIterationExpression().copy(), 
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
                remove(m_loop);
            }
        
        return super.doChange();
    }

}
