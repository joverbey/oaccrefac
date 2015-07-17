package edu.auburn.oaccrefac.core.change;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

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
        
        //Create pattern for matching loop headers
        IASTForStatement pattern = m_first.copy();
        pattern.setBody(new ArbitraryStatement());
        
        while (ASTUtil.getNextSibling(newnode) != null && !found) {
            newnode = ASTUtil.getNextSibling(newnode);
            m_second = ASTUtil.findOne(newnode, IASTForStatement.class);
            found = (m_second != null);
            
            //Check to make sure the first and second loops have same headers
            Map<String, String> varmap = ASTMatcher.unify(pattern, m_second);
            if (varmap != null) {
                for (String key : varmap.keySet()) {
                    //The map returned contains name mapping that
                    //tells which names would make the two patterns equal
                    if (!varmap.get(key).equals(key)) {
                        found = false;
                    }
                }
                found = true;
            } else {
                found = false;
            } 
        }

        if (!found) {
            init.addFatalError("There is no for loop for fusion to be possible.");
            return init;
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
                this.safeInsertBefore(body_rewriter, body, null, modifyChild(child));
            }
        } else {
            this.safeInsertBefore(body_rewriter, body, null, body_second);
        }
        
        this.safeRemove(rewriter, m_second);
        return rewriter;
    }

    private IASTNode modifyChild(IASTNode child) {

        if (isVaribleDeclaration(child) 
                && isAssignmentDeclaration(child)) {
            return modifyVariableDeclaration(child);
        } else {
            return child.copy();
        }
    }
    
    private boolean isAssignmentDeclaration(IASTNode child) {
        IASTEqualsInitializer decl = 
                ASTUtil.findOne(child, IASTEqualsInitializer.class);
        if (decl == null)
            return false;
        IASTInitializerClause clause = decl.getInitializerClause();
        return (clause instanceof IASTExpression);
    }

    private boolean isVaribleDeclaration(IASTNode child) {
        return (child instanceof IASTDeclarationStatement);
    }

    private IASTNode modifyVariableDeclaration(IASTNode child) {
        IASTName name = 
                ASTUtil.findOne(child, IASTName.class);
        if (name != null) {
            IASTEqualsInitializer decl = 
                    ASTUtil.findOne(child, IASTEqualsInitializer.class);
            IASTInitializerClause clause = decl.getInitializerClause().copy();
            name = name.copy();
            
            ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
            IASTBinaryExpression expr = factory.newBinaryExpression(
                        IASTBinaryExpression.op_assign, 
                        factory.newIdExpression(name), 
                        (IASTExpression) clause);
            return factory.newExpressionStatement(expr);
        } else {
            throw new UnsupportedOperationException("Modifying variable"
                    + " declaration without a name...is this possible in C/C++?");
        }
    }



}
