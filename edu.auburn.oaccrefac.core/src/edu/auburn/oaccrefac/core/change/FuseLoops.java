package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class FuseLoops extends ForLoopChange {

    private IASTForStatement m_first;
    private IASTForStatement m_second;
    
    public FuseLoops(IASTRewrite rewriter, IASTForStatement loop) {
        super(rewriter, loop);
        m_first = loop;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        // This gets the selected loop to re-factor.
        boolean found = false;
        IASTNode newnode = m_first;
        while (ASTUtil.getNextSibling(newnode) != null && !found) {
            newnode = ASTUtil.getNextSibling(newnode);
            m_second = ASTUtil.findOne(newnode, IASTForStatement.class);
            found = (m_second != null);
            
            // FIXME
            // We probably need to do something more to check the
            // actual headers so that they align.
        }

        if (!found) {
            init.addFatalError("There is no for loop for fusion to be possible.");
        }
        return init;
    }
    
    @Override
    protected IASTRewrite doChange(IASTRewrite rewriter) {
        IASTStatement body = m_first.getBody();
        IASTRewrite body_rewriter = rewriter;
        if (!(body instanceof IASTCompoundStatement)) {
            ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
            IASTCompoundStatement newBody = factory.newCompoundStatement();
            newBody.addStatement(body.copy());
            body_rewriter = this.safeReplace(rewriter, body, newBody);
            body = newBody;
        }
        
        IASTStatement body_second = m_second.getBody();
        if (body_second instanceof IASTCompoundStatement) {
            IASTNode chilluns[] = body_second.getChildren();
            for (IASTNode child : chilluns) {
                this.safeInsertBefore(body_rewriter, body, null, child.copy());
            }
        } else {
            this.safeInsertBefore(body_rewriter, body, null, body_second);
        }
        
        this.safeRemove(rewriter, m_second);
        return rewriter;
    }

}