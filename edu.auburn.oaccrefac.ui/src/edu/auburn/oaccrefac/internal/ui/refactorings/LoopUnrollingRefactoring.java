package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.ArrayList;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
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
	protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {	    	    
	    IASTStatement body = getLoop().getBody();
        //if the body is empty, exit out -- pointless to unroll.
        if (body == null || body instanceof IASTNullStatement)
            return;
        
        
        IASTCompoundStatement body_compound = null;
        //if the body is not within braces, i.e. short-cut
        //loop pattern, make it a compounded statement
        if (!(body instanceof IASTCompoundStatement)) {
            ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
            body_compound = factory.newCompoundStatement();
            body_compound.addStatement(body.copy());
        } else {
            body_compound = (IASTCompoundStatement) (body.copy());
        }
	    
	    //get the loop upper bound from AST
	    int upper = getUpperBoundValue();
	    
	    
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
	        setUpperBound(upper, rewriter);
	    }
	    
	  //Now non-divisible loops are now treated as divisible loops.
        //Unroll the loop however many times specified.
        unroll((IASTCompoundStatement) body_compound);
        rewriter.replace(getLoop().getBody(), body_compound, null);
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
	    //Find the first name in the initializer statement
	    //We will use this as the name to replace in corresponding accesses
	    IASTName var_name = findFirstName(getLoop().getInitializerStatement());
	    	    
	    //Get the insertion locations
	    IASTNode insert_parent = getLoop().getParent();
	    IASTNode insert_before = getNextSibling(getLoop());
	    
	    
	    /*
	     * The trailer consists of a number of extra loop iterations after 
	     * the loop to make up for the non-divisibility of the loop body
	     * bounds in accordance with the given loop factor. We must copy
	     * each individual statement of the body to the location right before
	     * the next sibling of the loop.
	     */
	    for (int i = 0; i < extras; i++) {
	        for (IASTNode child : body.getChildren())  {
	            IASTNode newInsert = child.copy();
	            int replace_const = (upperBound-(extras-i));
	            ASTRewrite newRewriter = rewriter.insertBefore(insert_parent, insert_before, newInsert, null);
	            findNameReplaceWithLiteral(var_name, newInsert, replace_const, newRewriter);
	        }
	    }

	}
	
	private void findNameReplaceWithLiteral(IASTName find, IASTNode tree, int replacement, ASTRewrite rewriter) {
	    
	    /**
	     * The NameVisitor visits all names within a tree. In this
	     * context, it is used by the AccessVisitor class in order
	     * to find all names within an array subscript. For example,
	     * a[i] = 0; -- it would find 'i' as an IASTName object and
	     * add it to the 'name_list'. After completion, the name_list
	     * will be filled with IASTName objects to be replaced.
	     */
	    class NameVisitor extends ASTVisitor {
            private ArrayList<IASTName> name_list = null;
            
            public NameVisitor() {
                name_list = new ArrayList<IASTName>();
                //want to find names within access expressions
                shouldVisitNames = true;
            }
            
            @Override
            public int visit(IASTName visitor) {
                name_list.add(visitor);
                return PROCESS_CONTINUE;
            }
        }
	    
	    /**
	     * The AccessVisitor visits all expressions and specifically
	     * looks for IASTArraySubscriptExpression objects within a tree.
	     * From there, the subscript expression does another search within
	     * its own tree to look for any IASTName objects.
	     */
	    class AccessVisitor extends ASTVisitor {
            private IASTArraySubscriptExpression access = null;
            private NameVisitor name_visitor = null;
            
            public AccessVisitor() {
                name_visitor = new NameVisitor();
                //want to find array access expressions
                shouldVisitExpressions = true;
            }
            
            @Override
            public int visit(IASTExpression visitor) {
                if (visitor instanceof IASTArraySubscriptExpression) {
                    access = (IASTArraySubscriptExpression) visitor;
                    access.accept(name_visitor);
                }
                return PROCESS_CONTINUE; //we want to visit all possibilities
            }
        }
	    
	    //Create a visitor and process on all instances of array subscript
	    //expressions. This way we can create a list of all names we need
	    //to replace within this tree.
        AccessVisitor v = new AccessVisitor();
        tree.accept(v);
        
        //Create our new literal node containing our constant
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTLiteralExpression const_replace = factory.newLiteralExpression(
                IASTLiteralExpression.lk_integer_constant, 
                replacement+"");
        
        //Iterate through list of names, replacing all matching
        //names with the literal constant nodes.
        //NOTE: The names IASTName are all children of IASTIdExpression,
        //therefore, we must replace the parent instead. We should probably
        //have a list of IdExpressions instead and compare them...but oh well?
        for (IASTName name : v.name_visitor.name_list) {
            String left = new String(name.getSimpleID());
            String right = new String(find.getSimpleID());
            if (left.compareTo(right) == 0) {
                rewriter.replace(name.getParent(), const_replace, null);
            }
        }
    }
	
	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		return null;
	}

}
