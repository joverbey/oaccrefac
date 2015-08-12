package edu.auburn.oaccrefac.core.change;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * This class defines the base strategy interface to be derived
 * from for changes made to a for loop.
 * @author Adam Eichelkraut
 *
 */
public abstract class ForLoopChange extends ASTChange {
    
    //Members
    private IASTForStatement m_loop;
    
    /**
     * Constructor that takes a for-loop and a rewriter (for base)
     * @author Adam Eichelkraut
     * @param rewriter -- rewriter to be given to base class
     * @param loopToChange -- loop to change
     * @throws IllegalArgumentException if the for loop is null
     */
    public ForLoopChange(IASTTranslationUnit tu, IASTRewrite rewriter, IASTForStatement loopToChange) {
        super(tu, rewriter);
        if (loopToChange == null) {
            throw new IllegalArgumentException("Argument loop cannot be null!");
        }
        m_loop = loopToChange;
    }
    
    /**
     * Gets the loop set from constructor
     * @author Adam Eichelkraut
     * @return loop to change
     */
    public IASTForStatement getLoopToChange() {
        return m_loop;
    }
    
    /**
     * Sets the loop to change
     * @author Adam Eichelkraut
     * @param newLoop, not null
     * @throws IllegalArgumentException if argument is null
     */
    public void setLoopToChange(IASTForStatement newLoop) {
        if (newLoop == null) {
            throw new IllegalArgumentException("Argument loop cannot be null!");
        }
        m_loop = newLoop;
    }
    
    /**
     * Generates a new name that is not within the scope provided by
     * taking a base name and appending '_#', where the '#' is a number
     * until it isn't in the scope.
     * @author Adam Eichelkraut
     * @param base -- base name to build new name off of
     * @param scope -- scope to check name against
     * @return -- new name object
     */
    protected final IASTName generateNewName(IASTName base, IScope scope) {
        String base_str = new String(base.getSimpleID());
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        int diffcounter = 0;
        String gen_str = base_str+"_"+diffcounter;
        IASTName gen_name = factory.newName(gen_str.toCharArray());
        while (ASTUtil.isNameInScope(gen_name, scope)) {
            diffcounter++;
            gen_str = base_str+"_"+diffcounter;
            gen_name = factory.newName(gen_str.toCharArray());
        }
        return gen_name;
    }
    
    /**
     * Returns a generated variable declaration given a name, type, and optional
     * equals initializer.
     * For example it could create 'int x = 12'
     * @param varname -- name of the new variable
     * @param type -- type (integer from {@link IASTSimpleDeclSpecifier})
     * @param right_equals -- optional initializer for variable (null if none)
     * @return -- new variable declaration statement node
     */
    protected IASTDeclarationStatement generateVariableDecl(
            IASTName varname, int type, IASTInitializer right_equals) {
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTSimpleDeclSpecifier declSpecifier = factory.newSimpleDeclSpecifier();
        declSpecifier.setType(type);
        IASTSimpleDeclaration declaration = factory.newSimpleDeclaration(declSpecifier);
        if (right_equals != null) {
            IASTDeclarator declarator = factory.newDeclarator(varname);
            declarator.setInitializer(right_equals);
            declaration.addDeclarator(declarator);
        }
        return factory.newDeclarationStatement(declaration);
    }
    
    /**
     * Returns the upper bound expression for a loop, assuming it is 
     * supported by the list of patterns defined
     * @param loop -- loop in which to get upper bound
     * @return -- upper bound expression
     * @throws UnsupportedOperationException if pattern is not supported
     */
    protected IASTExpression getUpperBoundExpression(IASTForStatement loop) {
        IASTExpression ub = null;
        if (loop.getConditionExpression() instanceof IASTBinaryExpression) {
            IASTBinaryExpression cond_be = (IASTBinaryExpression) loop.getConditionExpression();
            ub = (IASTExpression)cond_be.getOperand2();
        } else {
            throw new UnsupportedOperationException("Non-binary conditional statements unsupported");
        }
        return ub;
    }
    
//    /**
//     * Exchanges two loop headers. 
//     * Replaces the headers from two
//     * loops. It also handles swapping pragmas.
//     * @param rewriter -- rewriter to do swaps with
//     * @param loop1 -- first loop to swap headers
//     * @param loop2 -- second loop to swap headers
//     * @return -- returns rewriter used
//     */
//    protected void exchangeLoopHeaders(IASTRewrite rewriter,
//            IASTForStatement loop1, IASTForStatement loop2) {
//
//        this.replace(loop1.getInitializerStatement().getFileLocation().getNodeOffset(),
//                loop1.getInitializerStatement().getFileLocation().getNodeLength(),
//                loop2.getInitializerStatement().getRawSignature());
//        this.replace(loop1.getConditionExpression().getFileLocation().getNodeOffset(),
//                loop1.getConditionExpression().getFileLocation().getNodeLength(),
//                loop2.getConditionExpression().getRawSignature());
//        this.replace(loop1.getIterationExpression().getFileLocation().getNodeOffset(),
//                loop1.getIterationExpression().getFileLocation().getNodeLength(),
//                loop2.getIterationExpression().getRawSignature());
//        this.replace(loop2.getInitializerStatement().getFileLocation().getNodeOffset(),
//                loop2.getInitializerStatement().getFileLocation().getNodeLength(),
//                loop1.getInitializerStatement().getRawSignature());
//        this.replace(loop2.getConditionExpression().getFileLocation().getNodeOffset(),
//                loop2.getConditionExpression().getFileLocation().getNodeLength(),
//                loop1.getConditionExpression().getRawSignature());
//        this.replace(loop2.getIterationExpression().getFileLocation().getNodeOffset(),
//                loop2.getIterationExpression().getFileLocation().getNodeLength(),
//                loop1.getIterationExpression().getRawSignature());
//        this.finalizeChanges();
//        
//    }
    
}
