package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class UnrollLoop extends ForLoopChange {

    private int m_unrollFactor;
    private Long m_upperBound;
    
    public UnrollLoop(IASTRewrite rewriter, IASTForStatement loop, int unrollFactor) {
        super(rewriter, loop);
        m_unrollFactor = unrollFactor;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        if (m_unrollFactor <= 0) {
            init.addFatalError("Invalid loop unroll factor! (<= 0)");
            return init;
        }
        
        //If the upper bound is not a constant, we cannot do loop unrolling
        IASTForStatement loop = getLoopToChange();
        IASTFunctionDefinition enclosing = 
                ASTUtil.findNearestAncestor(loop, IASTFunctionDefinition.class);
        ConstantPropagation constantprop_ub = new ConstantPropagation(enclosing);
        IASTExpression ub_expr = ((IASTBinaryExpression)loop.getConditionExpression()).getOperand2();
        m_upperBound = constantprop_ub.evaluate(ub_expr);
        if (m_upperBound == null) {
            init.addFatalError("Upper bound is not a constant value. Cannot perform unrolling!");
            return init;
        }
        
        IASTStatement body = loop.getBody();
        //if the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement) {
            init.addFatalError("Loop body is empty -- nothing to unroll!");
            return init;
        }
        return init;
    }
    
    @Override
    protected IASTRewrite doChange(IASTRewrite rewriter) {
        IASTForStatement loop = this.getLoopToChange();
        
        //if the init statement is a declaration, move the declaration 
        //to outer scope if possible and continue...
        if (ASTUtil.findOne(loop, IASTDeclarationStatement.class) != null)
            adjustInit(rewriter, loop);
        
        //get the loop upper bound from AST, if the conditional statement is '<=', 
        //change it and adjust the bound to include a +1.
        int upper = m_upperBound.intValue();
        int cond_offset = 0;
        IASTBinaryExpression condition = 
                (IASTBinaryExpression) loop.getConditionExpression();
        if (condition.getOperator() == IASTBinaryExpression.op_lessEqual) {
            upper = upper + 1;
            cond_offset = cond_offset + 1;
        }
        
        //Number of extra iterations to add after loop based on divisibility.
        int extras = upper % m_unrollFactor;
        if (extras != 0) {
            addTrailer(rewriter, loop, upper, extras);
            cond_offset = cond_offset - extras;
        }
        
        offsetCondition(rewriter, condition, cond_offset);
        
        //Now non-divisible loops are now treated as divisible loops.
        //Unroll the loop however many times specified.
        unroll(rewriter, loop);
        
        return rewriter;
    }
        
    private void adjustInit(IASTRewrite rewriter, IASTForStatement loop) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTName counter_name = ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class);
        try {
            IScope scope = loop.getScope().getParent();
            if (!ASTUtil.isNameInScope(counter_name, scope)) {
                this.safeInsertBefore(rewriter, 
                        loop.getParent(), loop, loop.getInitializerStatement().copy());
                this.safeReplace(rewriter, 
                        loop.getInitializerStatement(), factory.newNullStatement());
            }
        } catch (DOMException e) {
            e.printStackTrace();
        } //DOMException
    }
    
    private void offsetCondition(IASTRewrite rewriter,
            IASTBinaryExpression condition, int amount) {
        
        int operator = IASTBinaryExpression.op_plus;
        if (amount < 0) {
            operator = IASTBinaryExpression.op_minus;
            amount = amount * -1;
        }
        IASTBinaryExpression condcopy = condition.copy();
        condcopy.setOperator(IASTBinaryExpression.op_lessThan);
        
        if (amount > 0) {
            IASTExpression cond_right = condcopy.getOperand2();
    
            ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
            IASTLiteralExpression amtLit = factory.newLiteralExpression(
                    IASTLiteralExpression.lk_integer_constant, amount+"");
            
            IASTBinaryExpression appendedOperation = factory.newBinaryExpression(
                    operator, cond_right, amtLit);
            IASTUnaryExpression parenth = factory.newUnaryExpression(
                    IASTUnaryExpression.op_bracketedPrimary, appendedOperation);
            condcopy.setOperand2(parenth);
        }
        
        this.safeReplace(rewriter, condition, condcopy);
    }
    
    private IASTRewrite addTrailer(IASTRewrite rewriter, IASTForStatement loop,
            int upperBound, int extras) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTStatement body = loop.getBody();
        IASTExpression iter_expr = loop.getIterationExpression();
        IASTExpressionStatement iter_exprstmt = factory.newExpressionStatement(iter_expr.copy());
        
        IASTNode insertBefore = ASTUtil.getNextSibling(loop);
        for (int i = 0; i < extras; i++) {
            this.safeInsertBefore(rewriter, 
                    loop.getParent(), insertBefore, iter_exprstmt);
            
            if (body instanceof IASTCompoundStatement) {
                IASTStatement[] chilluns = ((IASTCompoundStatement) body).getStatements();
                for (IASTStatement child : chilluns) {
                    this.safeInsertBefore(rewriter, 
                            loop.getParent(), insertBefore, child.copy());
                }
            } else {
                this.safeInsertBefore(rewriter, 
                        loop.getParent(), insertBefore, body.copy());
            }
        }
        this.safeInsertBefore(rewriter, 
                loop.getParent(), insertBefore, iter_exprstmt);
        return rewriter;
    }

    private IASTRewrite unroll(IASTRewrite rewriter, IASTForStatement loop) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTExpression iter_expr = loop.getIterationExpression();
        IASTExpressionStatement iter_exprstmt = factory.newExpressionStatement(iter_expr.copy());
        
        IASTStatement body = loop.getBody();
        IASTNode[] chilluns = body.getChildren();
        
        IASTCompoundStatement newBody = factory.newCompoundStatement();
        for (int i = 0; i < m_unrollFactor; i++) {
            if (body instanceof IASTCompoundStatement) {
                for (int j = 0; j < chilluns.length; j++) {
                    if (chilluns[j] instanceof IASTStatement)
                        newBody.addStatement((IASTStatement)chilluns[j].copy());
                }
            } else {
                newBody.addStatement(body.copy());
            }
            if (i != m_unrollFactor-1) {
                newBody.addStatement(iter_exprstmt);
            }
        }
        
        this.safeReplace(rewriter, body, newBody);
        
        return rewriter;
    }
    

}
