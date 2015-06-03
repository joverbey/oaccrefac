package edu.auburn.oaccrefac.core.newtmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.IndexExpression;
import edu.auburn.oaccrefac.internal.core.InductionVariable;
import edu.auburn.oaccrefac.internal.core.Matrix;
import edu.auburn.oaccrefac.internal.core.Pair;

/**
 * @author Alexander Calvert
 * 
 * Class to encapsulate a for loop data dependence
 *
 */
@SuppressWarnings("restriction")
public class ForLoopDependenceSystem {

    private Matrix inequalityMatrix;
    private Matrix equalityMatrix;
    private Map<String, InductionVariable> inductionVariables;    

    /** 
     * Constructor
     * takes a for statement in, setting up this instance's dependence 
     * dependence system of equations if it is valid 
     * 
     * @param outerLoop the outer for loop to be 
     */
    public ForLoopDependenceSystem(CPPASTForStatement outerLoop) {
        //consolidate most of the original code from Calvin's ForLoopDependence
        //to this constructor
        //should check for valid/invalid for statements and bodies and generate both
        //matrices to represent the dependences in this loop
        
        //check if loop is perfectly nested
        if(!isForLoopPerfectlyNested(outerLoop)) {
            ASTUtil.raise("Loop must be perfectly nested", outerLoop);
        }
        
        //check if only statements on innermost loop are assignments

        p(areAllInnermostStatementsValid(outerLoop));
        
        //for now, assume that there is only one statement in the body and  
        //that it's an assignment
        
        
        
    }

    public Matrix getInequalityMatrix() {
        return inequalityMatrix;
    }

    public Matrix getEqualityMatrix() {
        return equalityMatrix;
    }
    
    private void p(Object o) {
        System.out.println(o);
    }
    
    private boolean isForLoopPerfectlyNested(CPPASTForStatement outerLoop) {
        IASTNode[] loopChildren = outerLoop.getChildren();
        if(!doesForLoopContainForLoopChild(outerLoop)) {            
            return true;
        }
        else { //outerLoop has for loop children
            if(outerLoop.getBody() instanceof IASTCompoundStatement) {
                IASTNode[] children = outerLoop.getBody().getChildren();
                if(children.length == 1 && children[0] instanceof CPPASTForStatement) {
                    return isForLoopPerfectlyNested((CPPASTForStatement) children[0]);
                }
                else {
                    return false; 
                }
            }
            else {
                return isForLoopPerfectlyNested((CPPASTForStatement) outerLoop.getBody());
            }
        }
    }
    private boolean doesForLoopContainForLoopChild(CPPASTForStatement loop) {
        
        class Visitor extends ASTVisitor {

            public Visitor() {
                shouldVisitStatements = true;
            }

            @Override
            public int visit(IASTStatement statement) {
                if(statement instanceof CPPASTForStatement) {
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }
        }
        //if we've aborted, it's because we found a for statement
        return !loop.getBody().accept(new Visitor());

    }
    
    /** Assumes loops are perfectly nested
     * Check if all innermost statements are valid
     *  currently, a valid statement is either an assignment statement or null
     * @param outerLoop
     * @return
     */
    private boolean areAllInnermostStatementsValid(CPPASTForStatement outerLoop) {
        IASTNode body = outerLoop.getBody();
        if(body instanceof IASTCompoundStatement) {
            p("compound stmt body");
            if(body.getChildren().length == 0) {
                p("body has no children");
                return true;
            }
            //assuming perfect nesting, so only check children[0]
            else if(body.getChildren()[0] instanceof CPPASTForStatement) {
                p("body has a for stmt child, calling recursively");
                return areAllInnermostStatementsValid((CPPASTForStatement) body.getChildren()[0]);
            }
            else {
                //check if all children are assignments or null stmts
                for(IASTNode child : body.getChildren()) {
                    //to be an asgt, must be an expr stmt with a bin expr child, 
                    //which has asgt operator
                    if(child instanceof IASTExpressionStatement 
                            && child.getChildren().length > 0
                            && child.getChildren()[0] instanceof IASTBinaryExpression
                            && ((IASTBinaryExpression) child.getChildren()[0]).getOperator() == IASTBinaryExpression.op_assign) {
                        
                        p("found binex child");
                       continue;
                    }
                    else if(child instanceof IASTNullStatement) {
                        p("found null child");
                        continue;
                    }
                    else {
                        p("child is not an assignment or null");
                        return false;
                    }
                }
                return true;
            }
        }
        else if(body instanceof CPPASTForStatement) {
            p("simple body, for loop, calling recursively");
            return areAllInnermostStatementsValid((CPPASTForStatement) body);
        }
        else { //neither compound nor for statement - body is the only statement
            p("simple body, non-for loop");
            if(body instanceof IASTBinaryExpression) {
                if(((IASTBinaryExpression) body).getOperator() == IASTBinaryExpression.op_assign) {
                    p("only stmt is a asgt");
                    return true;
                }
            }
            //either not binary or not an assignment
            p("only stmt is not asgt");
            return false;
        }
    }    
    
    
}
