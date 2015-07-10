package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;

/**
 * This class defines the base strategy interface to be derived
 * from for changes made to a for loop.
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopChange extends ASTChange {
    
    private IASTForStatement m_loop;
    public ForLoopChange(ASTRewrite rewriter, IASTForStatement loopToChange) {
        super(rewriter);
        if (loopToChange == null) {
            throw new IllegalArgumentException("Argument loop cannot be null!");
        }
        m_loop = loopToChange;
    }
    
    public IASTForStatement getLoopToChange() {
        return m_loop;
    }
    
    public void setLoopToChange(IASTForStatement newLoop) {
        if (newLoop == null) {
            throw new IllegalArgumentException("Argument loop cannot be null!");
        }
        m_loop = newLoop;
    }
    
    protected IASTCompoundStatement ensureCompoundBody(ASTRewrite rewriter, 
            IASTForStatement loop) {
        IASTStatement body = loop.getBody();
        if (body instanceof IASTCompoundStatement) {
            return (IASTCompoundStatement) body;
        }
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTCompoundStatement cmpndBody = factory.newCompoundStatement();
        //(should only be one statement otherwise)
        cmpndBody.addStatement(body.copy());
        rewriter = this.safeReplace(rewriter, body, cmpndBody);
        return cmpndBody;
    }
    
    
    protected ASTRewrite exchangeLoopHeaders(ASTRewrite rewriter,
            IASTForStatement loop1, IASTForStatement loop2) {
        
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
        rewriter = this.safeReplace(rewriter,
                loop2.getIterationExpression(), 
                loop1.getIterationExpression());
        return rewriter;
    }
    
    
}
