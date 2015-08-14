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
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Inheriting from {@link ForLoopChange}, this class defines a loop unrolling
 * refactoring algorithm. Loop unrolling takes a sequential loop and 'unrolls'
 * the loop by copying the body of the loop multiple times in order to skip
 * testing the conditional expression more times than it has to.
 * 
 * For example,
 *      for (int i = 0; i < 19; i++) {
 *          a = 5;
 *      }
 * Refactors to:
 *      for (int i = 0; i < 18; i+=2) {
 *          a = 5;
 *          i++;
 *          a = 5;
 *      }
 *      i++;
 *      a = 5;
 *      i++;
 * 
 * The part at the end is called the 'trailer' and it holds the leftover
 * iterations that could not be satisfied by the unrolled loop body.
 * 
 * @author Adam Eichelkraut
 *
 */
public class UnrollLoop extends ForLoopChange {

    //Members
    private int m_unrollFactor;
    private Long m_upperBound;
    
    /**
     * Constructor.
     * @param rewriter -- rewriter associated with loop
     * @param loop -- loop in which to unroll 
     * @param unrollFactor -- how many times to unroll loop body (must be > 0)
     */
    public UnrollLoop(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loop, int unrollFactor) {
        super(tu, rewriter, loop);
        m_unrollFactor = unrollFactor;
    }
    
    @Override
    protected void doCheckConditions(RefactoringStatus init, IProgressMonitor pm) {
        //Check unroll factor validity...
        if (m_unrollFactor <= 0) {
            init.addFatalError("Invalid loop unroll factor! (<= 0)");
            return;
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
            return;
        }
        
        IASTStatement body = loop.getBody();
        //if the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement) {
            init.addFatalError("Loop body is empty -- nothing to unroll!");
            return;
        }
        return;
    }
    
    @Override
    protected void doChange() {
//        IASTForStatement loop = this.getLoopToChange();
//        
//        //if the init statement is a declaration, move the declaration 
//        //to outer scope if possible and continue...
//        if (ASTUtil.findOne(loop, IASTDeclarationStatement.class) != null)
//            adjustInit(rewriter, loop);
//        
//        //get the loop upper bound from AST, if the conditional statement is '<=', 
//        //change it and adjust the bound to include a +1.
//        int upper = m_upperBound.intValue();
//        int cond_offset = 0;
//        IASTBinaryExpression condition = 
//                (IASTBinaryExpression) loop.getConditionExpression();
//        if (condition.getOperator() == IASTBinaryExpression.op_lessEqual) {
//            upper = upper + 1;
//            cond_offset = cond_offset + 1;
//        }
//        
//        //Number of extra iterations to add after loop based on divisibility.
//        int extras = upper % m_unrollFactor;
//        if (extras != 0) {
//            addTrailer(rewriter, loop, upper, extras);
//            cond_offset = cond_offset - extras;
//        }
//        
//        offsetCondition(rewriter, condition, cond_offset);
//        
//        //Now non-divisible loops are now treated as divisible loops.
//        //Unroll the loop however many times specified.
//        unroll(rewriter, loop);
//        
//        return rewriter;
    }
    
//    /**
//     * Adjusts the initializer statement to where if the loop counter is a declaration,
//     * it places the declaration outside of the loop in order for the loop counter to
//     * persist after the loop has finished. This is so that the trailer will still have
//     * scope of the counter.
//     * @param rewriter -- rewriter associated with loop argument
//     * @param loop -- loop in which to modify
//     */
//    private void adjustInit(IASTRewrite rewriter, IASTForStatement loop) {
//        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
//        IASTName counter_name = ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class);
//        try {
//            IScope scope = loop.getScope().getParent();
//            if (!ASTUtil.isNameInScope(counter_name, scope)) {
//                this.safeInsertBefore(rewriter, 
//                        loop.getParent(), loop, loop.getInitializerStatement().copy());
//                this.safeReplace(rewriter, 
//                        loop.getInitializerStatement(), factory.newNullStatement());
//            }
//        } catch (DOMException e) {
//            e.printStackTrace();
//        } //DOMException
//    }
//    
//    /**
//     * Method offsets the condition expression by an amount, 
//     * meaning that it will make sure that the condition expression
//     * has a '<' sign and correctly offset by whatever amount. If the
//     * amount is negative, it subtracts; if positive, it adds.
//     * @param rewriter -- rewriter associated with condition expression
//     * @param condition -- condition expression to offset
//     * @param amount -- amount to offset
//     */
//    private void offsetCondition(IASTRewrite rewriter,
//            IASTBinaryExpression condition, int amount) {
//        
//        int operator = IASTBinaryExpression.op_plus;
//        if (amount < 0) {
//            operator = IASTBinaryExpression.op_minus;
//            amount = amount * -1;
//        }
//        IASTBinaryExpression condcopy = condition.copy();
//        condcopy.setOperator(IASTBinaryExpression.op_lessThan);
//        
//        if (amount > 0) {
//            IASTExpression cond_right = condcopy.getOperand2();
//    
//            ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
//            IASTLiteralExpression amtLit = factory.newLiteralExpression(
//                    IASTLiteralExpression.lk_integer_constant, amount+"");
//            
//            IASTBinaryExpression appendedOperation = factory.newBinaryExpression(
//                    operator, cond_right, amtLit);
//            IASTUnaryExpression parenth = factory.newUnaryExpression(
//                    IASTUnaryExpression.op_bracketedPrimary, appendedOperation);
//            condcopy.setOperand2(parenth);
//        }
//        
//        this.safeReplace(rewriter, condition, condcopy);
//    }
//    
//    /**
//     * Adds the extra loop bod(ies) after the loop in order to suffice the
//     * cases where the unrolling factor cannot divide evenly into the upper bound
//     * @param rewriter -- rewriter associated with loop argument
//     * @param loop -- loop after which to add trailer
//     * @param upperBound -- constant upper bound number
//     * @param extras -- how many extras to add
//     * @return -- rewriter? I don't know, man.
//     */
//    private IASTRewrite addTrailer(IASTRewrite rewriter, IASTForStatement loop,
//            int upperBound, int extras) {
//        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
//        IASTStatement body = loop.getBody();
//        IASTExpression iter_expr = loop.getIterationExpression();
//        IASTExpressionStatement iter_exprstmt = factory.newExpressionStatement(iter_expr.copy());
//        
//        //Get insertion point
//        IASTNode insertBefore = ASTUtil.getNextSibling(loop);
//        for (int i = 0; i < extras; i++) {
//            this.safeInsertBefore(rewriter, 
//                    loop.getParent(), insertBefore, iter_exprstmt);
//            
//            if (body instanceof IASTCompoundStatement) {
//                IASTStatement[] chilluns = ((IASTCompoundStatement) body).getStatements();
//                for (IASTStatement child : chilluns) {
//                    this.safeInsertBefore(rewriter, 
//                            loop.getParent(), insertBefore, child.copy());
//                }
//            } else {
//                this.safeInsertBefore(rewriter, 
//                        loop.getParent(), insertBefore, body.copy());
//            }
//        }
//        this.safeInsertBefore(rewriter, 
//                loop.getParent(), insertBefore, iter_exprstmt);
//        return rewriter;
//    }
//
//    /**
//     * Unrolling function that unrolls the loop body multiple times within itself,
//     * adding iteration expression statements inbetween body copies in order to
//     * maintain original behavior.
//     * @param rewriter -- rewriter associated with loop argument
//     * @param loop -- loop in which to unroll
//     * @return -- rewriter? I still don't know.
//     */
//    private IASTRewrite unroll(IASTRewrite rewriter, IASTForStatement loop) {
//        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
//        IASTExpression iter_expr = loop.getIterationExpression();
//        IASTExpressionStatement iter_exprstmt = factory.newExpressionStatement(iter_expr.copy());
//        
//        IASTStatement body = loop.getBody();
//        IASTNode[] chilluns = body.getChildren();
//        
//        IASTCompoundStatement newBody = factory.newCompoundStatement();
//        IASTRewrite body_rewriter = this.safeReplace(rewriter, body, newBody);
//        for (int i = 0; i < m_unrollFactor; i++) {
//            if (body instanceof IASTCompoundStatement) {
//                for (int j = 0; j < chilluns.length; j++) {
//                    if (chilluns[j] instanceof IASTStatement)
//                        this.safeInsertBefore(body_rewriter, newBody, null, chilluns[j].copy());
//                }
//            } else {
//                this.safeInsertBefore(body_rewriter, newBody, null, body.copy());
//            }
//            if (i != m_unrollFactor-1) {
//                this.safeInsertBefore(body_rewriter, newBody, null, iter_exprstmt);
//            }
//        }
//        
//        return rewriter;
//    }
    

}
