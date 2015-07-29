package edu.auburn.oaccrefac.core.change;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

/**
 * Inheriting from {@link ForLoopChange}, this class defines a loop fusion
 * refactoring algorithm. Loop fusion takes the bodies of two identical for-loop
 * headers and places them in one.
 * 
 *<p>As of now, these loop headers MUST be completely identical and MUST be
 * right next to each other. The reason they must be next to each other is because
 * there could be statements between the two loops that could change the meaning of
 * the program if two loops were merged into one.</p>
 * 
 * <p>
 * For example,
 *      for (int i = 0; i < 10; i++) {
 *          a[i] = b[i] + c[i];
 *      }
 *      for (int i = 0; i < 10; i++) {
 *          b[i-1] = a[i];
 *      }
 * Refactors to:
 *      for (int i = 0; i < 10; i++) {
 *          a[i] = b[i] + c[i];
 *          b[i-1] = a[i];
 *      }</p>
 *
 * @author Adam Eichelkraut
 *
 */
public class FuseLoops extends ForLoopChange {

    //Members
    private IASTForStatement m_first;
    private IASTForStatement m_second;
    private Map<String, IASTName> m_modifiedVariableDecls;
    
    /**
     * Constructor that takes a for-loop to perform fusion on
     * @param rewriter -- base rewriter for loop
     * @param loop -- loop to be fizzed
     */
    public FuseLoops(IASTRewrite rewriter, IASTForStatement loop) {
        super(rewriter, loop);
        m_first = loop;
        m_modifiedVariableDecls = new HashMap<String, IASTName>();
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
        
        //We make the first loop the loop body in which to merge
        //the two bodies. Make sure that it is a compound statement.
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
                //For each statement to be added to the first's body,
                //insert the return of 'modifyChild' on the child.
                this.safeInsertBefore(body_rewriter, body, null, modifyChild(child));
            }
        } else {
            //Otherwise, the second's body is just a single statement...
            this.safeInsertBefore(body_rewriter, body, null, modifyChild(body_second));
        }
        
        //Remove the second loop from AST, as both are merged at this point
        this.safeRemove(rewriter, m_second);
        return rewriter;
    }

    /**
     * Internal helper method called by the algorithm to potentially modify
     * the statement before being merged into the first's body. For example,
     * if the statement in the second's body is a redeclaration of a variable
     * in the first's body, then we need to change that somehow.
     * @author Adam Eichelkraut
     * 
     * TODO -- the current method is wrong. Instead of removing the declaration
     * of the variable, we need to change the name of the variable instead because
     * it doesn't handle the case that the two identical identifiers may be different types.
     * 
     * @param child -- statement from the second's body
     * @return -- copied, unfrozen node to be merged
     */
    private IASTNode modifyChild(IASTNode child) {

        if (isAssignmentDeclaration(child)) {
            //cast ensured from previous check
            return modifyVariableDeclaration(
                    (IASTDeclarationStatement) child);
        } else {
            return replaceModifiedVariables(child);
        }
    }
    
    /**
     * Determines if a given node is an assignment declaration by
     * looking for an {@link IASTEqualsInitializer} node within the
     * tree of the child parameter as well as make sure that the node
     * is a {@link IASTDeclarationStatement}.
     * @author Adam Eichelkraut
     * @param child -- node to look for
     * @return T/F whether it is assignment declaration
     */
    private boolean isAssignmentDeclaration(IASTNode child) {
        if (child instanceof IASTDeclarationStatement) {
            IASTEqualsInitializer eq = 
                    ASTUtil.findOne(child, IASTEqualsInitializer.class);
            if (eq == null)
                return false;
            IASTInitializerClause clause = eq.getInitializerClause();
            return (clause instanceof IASTExpression);
        } else {
            return false;
        }
    }

    /**
     * Modifies a variable declaration so that a variable declaration that
     * is conflicting with another within the first loop is able to be merged
     * @param child -- node to be modified
     * @return -- modified node
     */
    private IASTNode modifyVariableDeclaration(IASTDeclarationStatement child) {
        IASTDeclarator decltr = ASTUtil.findOne(child, IASTDeclarator.class);
        IASTName name = decltr.getName();
        //ensured from preconditions...
        IASTCompoundStatement first_body = (IASTCompoundStatement) m_first.getBody();
        
        if (ASTUtil.isNameInScope(name, first_body.getScope())) {
            IASTName newName = this.generateNewName(name, first_body.getScope());
            m_modifiedVariableDecls.put(new String(name.getSimpleID()), newName);
            IASTNode child_copy = child.copy();
            IASTDeclarator var_decltr = 
                    ASTUtil.findOne(child_copy, IASTDeclarator.class);
            var_decltr.setName(newName); //change name
            return child_copy;
        } else {
            return child.copy();
        }
    }
    
    /**
     * Method takes a child statement node from the body of the second
     * loop and replaces, if any, modified variable names that occured
     * before it.
     * @author Adam Eichelkraut
     * @param child -- node to replace variable names in
     * @return -- unfrozen, copy of child node with variable names replaced
     */
    private IASTNode replaceModifiedVariables(IASTNode child) {
        IASTNode modified = child.copy();
        List<IASTName> names = ASTUtil.find(modified, IASTName.class);
        for (IASTName name : names) {
            IASTName replacedName = 
                    m_modifiedVariableDecls.get(new String(name.getSimpleID()));
            if (replacedName != null) {
                IASTNode parent = name.getParent();
                if (parent instanceof IASTIdExpression) {
                    IASTIdExpression id = (IASTIdExpression) parent;
                    id.setName(replacedName);
                }
            }
        }
        return modified;
    }



}
