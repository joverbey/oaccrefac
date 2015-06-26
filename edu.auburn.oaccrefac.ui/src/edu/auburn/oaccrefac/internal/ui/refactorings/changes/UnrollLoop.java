package edu.auburn.oaccrefac.internal.ui.refactorings.changes;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class UnrollLoop extends CompoundModify {

    private IASTForStatement m_loop;
    private int m_unrollFactor;
    
    public UnrollLoop(IASTCompoundStatement compound, IASTForStatement loop, int unrollFactor) {
        super(compound);
        if (loop != null) {
            m_loop = loop;
        } else {
            throw new IllegalArgumentException("Cannot unroll null loop!");
        }
        m_unrollFactor = unrollFactor;
    }
    
    @Override
    protected RefactoringStatus doCheckConditions(RefactoringStatus init) {
        // TODO ...
        return null;
    }
    
    @Override
    protected void modifyCompound() {
        EnsureCompoundBody bodyChange = new EnsureCompoundBody(m_loop.copy());
        IASTForStatement loop = bodyChange.change();
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
        int upper = getUpperBoundValue();
        if (conditionLE(loop)) {
            upper = upper + 1;
            adjustCondition(loop, upper);
        }
        
        //Number of extra iterations to add after loop. Also
        //used to test if the loop is divisible by the unroll
        //factor.
        int extras = upper % m_unrollFactor;
        if (extras != 0) { //not divisible...
            addTrailer(upper, extras, body_compound);
            
            //re-adjust upper because now we
            //will treat the actual loop as if
            //it were divisible
            upper = upper - extras;
            
            //This also means we need to change the
            //loop's actual upper bound
            setUpperBound(loop, upper);
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
    
    private void adjustCondition(IASTForStatement loop, int newUpper) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTBinaryExpression be = (IASTBinaryExpression) loop.getConditionExpression();
        
        IASTLiteralExpression newUpperLiteral = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, 
                newUpper+"");
        IASTBinaryExpression new_cond = factory.newBinaryExpression(
                IASTBinaryExpression.op_lessThan, 
                be.getOperand1().copy(), 
                newUpperLiteral);
        loop.setConditionExpression(new_cond);
    }

    private boolean conditionLE(IASTForStatement loop) {
        //Check to see if the loop condition statement has '<=' operator
        if (loop.getConditionExpression() instanceof IASTBinaryExpression) {
            IASTBinaryExpression be = (IASTBinaryExpression) loop.getConditionExpression();
            if (be.getOperator() == IASTBinaryExpression.op_lessEqual) {
                return true;
            }
        }
        return false;
    }
    
    private void setUpperBound(IASTForStatement loop, int newUpperBound) {
        //Get a factory and create a new node to replace old one
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTLiteralExpression newExpr = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, 
                new Integer(newUpperBound).toString());
        
        IASTBinaryExpression cond = (IASTBinaryExpression) loop.getConditionExpression();
        cond.setOperand2(newExpr);
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
    
    protected IASTLiteralExpression getUpperBound() {
        IASTExpression cond = m_loop.getConditionExpression();
        if (cond instanceof IASTBinaryExpression) {
            IASTBinaryExpression binary_cond = (IASTBinaryExpression) cond;
            return (IASTLiteralExpression) (binary_cond.getChildren()[1]);
        } else {
            return null;
        }
    }

    protected int getUpperBoundValue() {
        return Integer.parseInt(new String(getUpperBound().getValue()));
    }


}
