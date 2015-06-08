package edu.auburn.oaccrefac.internal.ui.refactorings;

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
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;

/**
 * This class defines the implementation for refactoring a loop
 * so that it is unrolled. For example:
 * 
 * ORIGINAL:				REFACTORED:
 * int x;					|  int x;
 * for (x=0; x<100; x++)	|  for (x=0; x<100; x++) {
 *   delete(a[x]);			|    delete(a[x]); x++;
 *   						|    delete(a[x]); x++;
 *   						|	 delete(a[x]); x++;
 *   						|	 delete(a[x]); x++;
 *  						|	 delete(a[x]);
 *  						|  }
 * (Example taken from Wikipedia's webpage on loop unrolling)
 */
@SuppressWarnings("restriction")
public class LoopUnrollingRefactoring extends ForLoopRefactoring {

    private int m_unrollFactor;
    
	public LoopUnrollingRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
	}

    public void setUnrollFactor(int toSet) {
        if (toSet >= 0) {
            m_unrollFactor = toSet;
        } else {
            throw new IllegalArgumentException("Unroll factor <= 0");
        }
    }

	@Override
	protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {	
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
	    IASTForStatement loop = getLoop().copy();
	    	    
	    IASTStatement body = loop.getBody();
        //if the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement)
            return;
        
        
        IASTCompoundStatement body_compound = null;
        //if the body is not within braces, i.e. short-cut
        //loop pattern, make it a compounded statement
        if (!(body instanceof IASTCompoundStatement)) {
            body_compound = factory.newCompoundStatement();
            body_compound.addStatement(body.copy());
        } else {
            body_compound = (IASTCompoundStatement) (body.copy());
        }
        
        //if the init statement is a declaration,
        //move the declaration to outer scope if
        //possible and continue
        if (isInitDeclaration(loop)) {
            loop = adjustInit(loop, rewriter);
            if (loop == null) {
                return;
            }
        }
	    
	    //get the loop upper bound from AST, if the 
        //conditional statement is '<=', change it
	    int upper = getUpperBoundValue();
	    if (conditionLE(loop)) {
	        upper = upper + 1;
	        adjustCondition(loop, upper, rewriter);
	    }
	    
	    //Number of extra iterations to add after loop. Also
	    //used to test if the loop is divisible by the unroll
	    //factor.
	    int extras = upper % m_unrollFactor;
	    if (extras != 0) { //not divisible...
	        addTrailer(upper, extras, body_compound, rewriter);
	        
	        //re-adjust upper because now we
	        //will treat the actual loop as if
	        //it were divisible
	        upper = upper - extras;
	        
	        //This also means we need to change the
	        //loop's actual upper bound
	        setUpperBound(loop, upper, rewriter);
	    }
	    
	  //Now non-divisible loops are now treated as divisible loops.
        //Unroll the loop however many times specified.
        unroll((IASTCompoundStatement) body_compound);
        loop.setBody(body_compound);
        rewriter.replace(getLoop(), loop, null);
	}
	
	private IASTForStatement adjustInit(IASTForStatement loop, ASTRewrite rewriter) {
	    ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
	    IASTName counter_name = findFirstName(getLoop().getInitializerStatement());
	    IASTForStatement originalLoop = getLoop();
	    try {
            if (!isNameInScope(counter_name, originalLoop.getScope().getParent())) {
                rewriter.insertBefore(
                        originalLoop.getParent(), 
                        originalLoop, 
                        originalLoop.getInitializerStatement(), 
                        null);
                IASTForStatement newForLoop = factory.newForStatement
                        (factory.newNullStatement(), 
                                loop.getConditionExpression().copy(), 
                                loop.getIterationExpression().copy(), 
                                loop.getBody().copy());
                return newForLoop;
            }
        } catch (DOMException e) {
            e.printStackTrace();
        }
	    return null;
	}
	
	private boolean isInitDeclaration(IASTForStatement loop) {
	    IASTStatement init = loop.getInitializerStatement();
	    if (init instanceof IASTDeclarationStatement) {
	        return true;
	    }
	    return false;
	}
	
	private void adjustCondition(IASTForStatement loop, int newUpper, ASTRewrite rewriter) {
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
	
	private void setUpperBound(IASTForStatement loop, int newUpperBound, ASTRewrite rewriter) {
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
	    IASTExpression iter_expr = getLoop().getIterationExpression();
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
	
	private void addTrailer(int upperBound, int extras, IASTStatement body ,ASTRewrite rewriter) {
	    ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTExpression iter_expr = getLoop().getIterationExpression();
        IASTExpressionStatement iter_exprstmt = factory.newExpressionStatement(iter_expr.copy());
	    	    
	    //Get the insertion locations
	    IASTNode insert_parent = getLoop().getParent();
	    IASTNode insert_before = getNextSibling(getLoop());

	    for (int i = 0; i < extras; i++) {
	        rewriter.insertBefore(insert_parent, insert_before, iter_exprstmt, null);
	        for (IASTNode child : body.getChildren())
	            rewriter.insertBefore(insert_parent, insert_before, child.copy(), null);
	    }

	}


}
