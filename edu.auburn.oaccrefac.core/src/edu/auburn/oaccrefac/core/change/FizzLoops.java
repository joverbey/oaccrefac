package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class FizzLoops extends ForLoopChange {
    
    public FizzLoops(ASTRewrite rewriter, IASTForStatement loop) {
        super(rewriter, loop);
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        //If the loop doesn't have children, bail. Save some
        //energy by not doing the refactoring.
        IASTForStatement loop = this.getLoopToChange();
        if (!(loop.getBody() instanceof IASTCompoundStatement)) {
            init.addFatalError("Body does not have any statements"
                    + " therefore, loop fission is useless.");
            return init;
        }
        
        if (loop.getBody().getChildren().length < 2) {
            init.addWarning("Warning: Loop fission refactoring is "
                    + "useless with less than two statements in body.");
        }
        
        return init;
    }
    
    @Override
    protected ASTRewrite doChange(ASTRewrite rewriter) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        
        //Ensured from precondition...
        IASTForStatement loop = this.getLoopToChange();
        IASTCompoundStatement body = (IASTCompoundStatement) loop.getBody();
        
        /*
         * Over here, we are looking for all the statements in the body of for-loop
         * which can be put into individual for-loop of their own.
         */
        IASTNode insertBefore = ASTUtil.getNextSibling(loop);
        for (IASTStatement child : body.getStatements()) {
            IASTCompoundStatement newBody = factory.newCompoundStatement();
            newBody.addStatement(child.copy());
            IASTForStatement newForLoop = factory.newForStatement
                    (loop.getInitializerStatement().copy(), 
                            loop.getConditionExpression().copy(), 
                            loop.getIterationExpression().copy(), 
                            newBody);
            this.safeInsertBefore(rewriter, 
                    loop.getParent(), insertBefore, newForLoop);
        }
        
        //Remove the old loop from the statement list
        if (body.getStatements().length > 0) {
            this.safeRemove(rewriter, loop);
        }
        
        return rewriter;
    }

}
