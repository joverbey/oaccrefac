package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.INodeFactory;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

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
	protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
			throws CoreException, OperationCanceledException {
	    pm.subTask("Calculating modifications...");
	    
	    ASTRewrite rewriter = collector.rewriterForTranslationUnit(getAST());
	    
	    //get the loop upper bound from AST
	    int upper = getUpperBoundValue();
	    
	    //Number of extra iterations to add after loop. Also
	    //used to test if the loop is divisible by the unroll
	    //factor.
	    int extras = upper % m_unrollFactor;
	    if (extras != 0) { //not divisible...
	        //addTrailer(extras, rewriter);
	        
	        //re-adjust upper because now we
	        //will treat the actual loop as if
	        //it were divisible
	        upper = upper - extras;
	        
	        //This also means we need to change the
	        //loop's actual upper bound
	        setUpperBound(upper, rewriter);
	    }
	    
	    //Now non-divisible loops are now treated as divisible loops.
	    //Unroll the loop however many times specified.
	    unroll(rewriter);
	    
	}
	
	private void setUpperBound(int newUpperBound, ASTRewrite rewriter) {
	    //Get a factory and create a new node to replace old one
	    ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
	    IASTLiteralExpression newExpr = factory.newLiteralExpression(
	            IASTLiteralExpression.lk_integer_constant, 
                new Integer(newUpperBound).toString());
	    
	    //Get our old upper bound expression from AST
	    IASTLiteralExpression node_upper = getUpperBound();
	    
	    //Replace it!
	    rewriter.replace(node_upper, newExpr, null);
	}
	
	private void unroll(ASTRewrite rewriter) {	    
	    IASTStatement braced_stmt = getLoop().getBody();
	    IASTNode first_expr = braced_stmt.getChildren()[0];

	    IASTExpression iter_exp = getLoop().getIterationExpression();
        IASTNode iter_delimiter = rewriter.createLiteralNode(";");
	    
	    //We do unroll-1 since the first unroll is considered the
        //loop body itself without modifications. 
        //(i.e. a loop unroll of 1 would be no changes)
        for (int i = 0; i < (m_unrollFactor-1); i++) {
            //insert all children in the braced compound expression
            //before the first element
            for (IASTNode child : braced_stmt.getChildren()) {
                rewriter.insertBefore(braced_stmt, first_expr, child, null);
            }
            //Insert our loop iteration
            rewriter.insertBefore(braced_stmt, first_expr, iter_exp, null);
            //Add a semicolon after the iteration expression
            //(it doesn't already come with one since it came from loop init)
            rewriter.insertBefore(braced_stmt, first_expr, iter_delimiter, null);
        }
	}
	
	private void addTrailer(int upperBound, int extras, ASTRewrite rewriter) {
	    //Find the first name in the initializer statement
	    //We will use this as the name to replace in corresponding accesses
	    IASTName var_name = findFirstName(getLoop().getInitializerStatement());
	    	    
	    IASTStatement braced_stmt = getLoop().getBody();
	    for (IASTNode child : braced_stmt.getChildren()) {
	        IASTNode newInsert = child.copy();
	        //findNameReplaceLiteral(newInsert, var_name, )
	    }

	}
	
	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

}
