package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

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

public class UnrollLoop extends CompoundModify {

    private IASTForStatement m_loop;
    private int m_unrollFactor;
    private Long m_upperBound;
    
    public UnrollLoop(IASTCompoundStatement compound, IASTForStatement loop, int unrollFactor) {
        super(compound);
        m_loop = loop;
        m_unrollFactor = unrollFactor;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        if (m_loop == null) {
            init.addFatalError("Loop cannot be null!");
            return init;
        }
        
        if (m_unrollFactor <= 0) {
            init.addFatalError("Invalid loop unroll factor! (<= 0)");
            return init;
        }
        
        //If the upper bound is not a constant, we cannot do loop unrolling
        IASTFunctionDefinition enclosing = 
                ASTUtil.findNearestAncestor(getOriginal(), IASTFunctionDefinition.class);
        ConstantPropagation constantprop_ub = new ConstantPropagation(enclosing);
        IASTExpression ub_expr = ((IASTBinaryExpression)m_loop.getConditionExpression()).getOperand2();
        m_upperBound = constantprop_ub.evaluate(ub_expr);
        if (m_upperBound == null) {
            init.addFatalError("Upper bound is not a constant value. Cannot perform unrolling!");
            return init;
        }
        
        IASTStatement body = m_loop.getBody();
        //if the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement) {
            init.addFatalError("Loop body is empty -- nothing to unroll!");
            return init;
        }
        return init;
    }
    
    @Override
    protected void modifyCompound() {
        EnsureCompoundBody bodyChange = new EnsureCompoundBody(m_loop.copy());
        IASTForStatement loop = bodyChange.change(this);
        IASTCompoundStatement body_compound = (IASTCompoundStatement) loop.getBody();
        
        //if the init statement is a declaration,
        //move the declaration to outer scope if
        //possible and continue
        if (isInitDeclaration(loop)) {
            try {
                adjustInit(loop);
            } catch (DOMException e) {
                e.printStackTrace();
            }
        }
        
        //get the loop upper bound from AST, if the 
        //conditional statement is '<=', change it
        int upper = m_upperBound.intValue();
        if (isConditionLE(loop)) {
            upper = upper + 1;
            ((IASTBinaryExpression) loop.getConditionExpression())
                    .setOperator(IASTBinaryExpression.op_lessThan);
            adjustCondition(loop, 1, IASTBinaryExpression.op_plus);
        }
        
        //Number of extra iterations to add after loop. Also
        //used to test if the loop is divisible by the unroll
        //factor.
        int extras = upper % m_unrollFactor;
        if (extras != 0) { //not divisible...
            addTrailer(upper, extras, body_compound);
            
            //This also means we need to change the
            //loop's actual upper bound
            adjustCondition(loop, extras, IASTBinaryExpression.op_minus);
        }
        
      //Now non-divisible loops are now treated as divisible loops.
        //Unroll the loop however many times specified.
        unroll((IASTCompoundStatement) body_compound);
        replace(m_loop, loop);
    }
    
    private void adjustInit(IASTForStatement loop) throws DOMException {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTName counter_name = ASTUtil.findOne(loop.getInitializerStatement(), IASTName.class);
        IScope scope = m_loop.getScope().getParent(); //DOMException
        if (!ASTUtil.isNameInScope(counter_name, scope)) {
            insertBefore(loop.getInitializerStatement(), m_loop);
            loop.setInitializerStatement(factory.newNullStatement());
        }
    }
    
    private boolean isInitDeclaration(IASTForStatement loop) {
        IASTStatement init = loop.getInitializerStatement();
        if (init instanceof IASTDeclarationStatement)
            return true;
        return false;
    }
    
    private void adjustCondition(IASTForStatement loop, int amount, int binaryOp) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTBinaryExpression be = (IASTBinaryExpression) loop.getConditionExpression();
        IASTExpression cond_rightSide = be.getOperand2();
        IASTLiteralExpression amtLit = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, amount+"");
        IASTBinaryExpression newBinaryOp = factory.newBinaryExpression(
                binaryOp, cond_rightSide, amtLit);
        IASTUnaryExpression parenth = factory.newUnaryExpression(
                IASTUnaryExpression.op_bracketedPrimary, newBinaryOp);
        be.setOperand2(parenth);
    }

    private boolean isConditionLE(IASTForStatement loop) {
        //Check to see if the loop condition statement has '<=' operator
        if (loop.getConditionExpression() instanceof IASTBinaryExpression) {
            IASTBinaryExpression be = (IASTBinaryExpression) loop.getConditionExpression();
            if (be.getOperator() == IASTBinaryExpression.op_lessEqual) {
                return true;
            }
        }
        return false;
    }
    
    private void unroll(IASTCompoundStatement body) {   
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTExpression iter_expr = m_loop.getIterationExpression();
        IASTExpressionStatement iter_exprstmt = factory.newExpressionStatement(iter_expr.copy());
        
        //We do unroll-1 since the first unroll is considered the
        //loop body itself without modifications. 
        //(i.e. a loop unroll of 1 would be no changes)
        IASTNode[] chilluns = body.getChildren();
        for (int i = 0; i < (m_unrollFactor-1); i++) {
            //Insert our loop iteration
            body.addStatement(iter_exprstmt);
            //insert all children in the braced compound expression
            //before the first element
            for (IASTNode child : chilluns) {
                if (child instanceof IASTStatement) {
                    body.addStatement((IASTStatement) child.copy()); 
                }
            }
        }
    }
    
    private void addTrailer(int upperBound, int extras, IASTStatement body) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTExpression iter_expr = m_loop.getIterationExpression();
        IASTExpressionStatement iter_exprstmt = factory.newExpressionStatement(iter_expr.copy());
        
        for (int i = 0; i < extras; i++) {
            insertAfter(iter_exprstmt, m_loop);
            IASTNode[] chilluns = body.getChildren();
            for (int j = chilluns.length-1; j >=0; j--) {
                if (chilluns[j] instanceof IASTStatement)
                    insertAfter((IASTStatement) chilluns[j], m_loop);
            }
        }
        insertAfter(iter_exprstmt, m_loop);
    }


}
