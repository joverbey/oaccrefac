package edu.auburn.oaccrefac.internal.core;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryIntegerConstant;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

public class ForLoopUtil {

    // Patterns of for loops that are acceptable to refactor...
    //Patterns of for loops that are acceptable to refactor...
    private static String[] patterns = {
        "for (i = 0; i < 1; i++) ;",
        "for (int i = 0; i < 1; i++) ;",
        "for (i = 0; i <= 1; i++) ;",
        "for (int i = 0; i <= 1; i++) ;",
        "for (i = 0; i < 1; i+=1) ;",
        "for (int i = 0; i < 1; i+=1) ;",
        "for (i = 0; i <= 1; i+=1) ;",
        "for (int i = 0; i <= 1; i+=1) ;",
        "for (i = 0; i < 1; i=i+1) ;",
        "for (int i = 0; i < 1; i=i+1) ;",
        "for (i = 0; i <= 1; i=i+1) ;",
        "for (int i = 0; i <= 1; i=i+1) ;",
        "for (i = 0; i < j; i++) ;",
        "for (int i = 0; i < j; i++) ;",
        "for (i = 0; i <= j; i++) ;",
        "for (int i = 0; i <= j; i++) ;",
        "for (i = 0; i < j; i+=1) ;",
        "for (int i = 0; i < j; i+=1) ;",
        "for (i = 0; i <= j; i+=1) ;",
        "for (int i = 0; i <= j; i+=1) ;",
        "for (i = 0; i < j; i=i+1) ;",
        "for (int i = 0; i < j; i=i+1) ;",
        "for (i = 0; i <= j; i=i+1) ;",
        "for (int i = 0; i <= j; i=i+1) ;",
    };

    /**
     * Method matches the parameter with all of the patterns defined in the pattern string array above. It parses each
     * string into a corresponding AST and then uses a pattern matching utility to match if the pattern is loosely
     * synonymous to the matchee. (Basically, we have to check some pre-conditions before refactoring or else we could
     * run into some hairy cases such as an array subscript expression being in the initializer statement...which would
     * be a nightmare to refactor).
     * 
     * @param matchee
     *            -- tree or AST to match
     * @return Boolean describing whether the matchee matches any supported pattern
     * @throws CoreException
     */
    public static boolean isCountedLoop(IASTForStatement matchee) {
        class LiteralReplacer extends ASTVisitor {
            public LiteralReplacer() {
                shouldVisitExpressions = true;
            }

            @Override
            public int visit(IASTExpression visitor) {
                if (visitor instanceof IASTLiteralExpression && visitor.getParent() != null) {
                    IASTLiteralExpression expr = (IASTLiteralExpression) visitor;
                    if (expr.getParent() instanceof IASTBinaryExpression)
                        ((IASTBinaryExpression) expr.getParent()).setOperand2(new ArbitraryIntegerConstant());
                    else if (expr.getParent() instanceof IASTEqualsInitializer)
                        ((IASTEqualsInitializer) expr.getParent()).setInitializerClause(new ArbitraryIntegerConstant());
                }
                return PROCESS_CONTINUE;
            }
        }

        for (String pattern : patterns) {
            IASTForStatement forLoop = (IASTForStatement) ASTUtil.parseStatementNoFail(pattern);
            IASTForStatement pattern_ast = forLoop.copy(CopyStyle.withoutLocations);
            pattern_ast.accept(new LiteralReplacer());
            pattern_ast.setBody(new ArbitraryStatement());
            if (ASTMatcher.unify(pattern_ast, matchee) != null)
                return true;
        }
        return false;
    }

    /**
     * Method takes in a tree and traverses to determine whether the tree contains a break or continue statement.
     * 
     * @param tree
     *            -- the tree to traverse
     * @return -- true/false on successful find
     */
    public static boolean containsBreakorContinue(IASTNode tree) {
        class UnsupportedVisitor extends ASTVisitor {
            public UnsupportedVisitor() {
                shouldVisitStatements = true;
                shouldVisitExpressions = true;
            }

            @Override
            public int visit(IASTStatement statement) {
                if (statement instanceof IASTBreakStatement || statement instanceof IASTContinueStatement
                        || statement instanceof IASTGotoStatement)
                    return PROCESS_ABORT;
                return PROCESS_CONTINUE;
            }
        }
        return (!tree.accept(new UnsupportedVisitor()));
    }

    public static boolean isPerfectLoopNest(IASTForStatement outerLoop) {
        if (!doesForLoopContainForLoopChild(outerLoop)) {
            return true;
        }

        if (outerLoop.getBody() instanceof IASTCompoundStatement) {
            IASTNode[] children = outerLoop.getBody().getChildren();
            if (children.length == 1 && children[0] instanceof IASTForStatement) {
                return isPerfectLoopNest((IASTForStatement) children[0]);
            } else {
                return false;
            }
        } else {
            return isPerfectLoopNest((IASTForStatement) outerLoop.getBody());
        }
    }

    private static boolean doesForLoopContainForLoopChild(IASTForStatement loop) {
        class Visitor extends ASTVisitor {
            public Visitor() {
                shouldVisitStatements = true;
            }

            @Override
            public int visit(IASTStatement statement) {
                if (statement instanceof IASTForStatement) {
                    return PROCESS_ABORT;
                }
                return PROCESS_CONTINUE;
            }
        }

        // If we've aborted, it's because we found a for statement
        return !loop.getBody().accept(new Visitor());
    }
    
    /**
     * This method (which baffles me as to why there isn't one of these in the IASTNode class, but whatever) returns the
     * next sibling after itself with respect to its parent.
     * 
     * @param n
     *            node in which to find next sibling
     * @return IASTNode of next sibling or null if last child
     */
    public static IASTNode getNextSibling(IASTNode n) {
        if (n.getParent() != null) {
            IASTNode[] chilluns = n.getParent().getChildren();
            for (int i = 0; i < chilluns.length; i++) {
                if (n == chilluns[i] && i < (chilluns.length - 1)) {
                    return chilluns[i + 1];
                }
            }
        }
        return null;
    }
    
    /** Assumes loops are perfectly nested
     * @param outerLoop
     * @return
     */
    public static IASTStatement getInnermostLoopBody(IASTForStatement outerLoop) {
        IASTStatement body = outerLoop.getBody();
        if(body instanceof IASTForStatement) {
            return getInnermostLoopBody((IASTForStatement) body);
        }
        else if(body instanceof IASTCompoundStatement) {
            IASTNode[] children = ((IASTCompoundStatement) body).getChildren();
            if(children.length == 1 && children[0] instanceof IASTForStatement) {
                return getInnermostLoopBody((IASTForStatement) children[0]);
            }
            else {
                return body;
            }
        }
        else {
            return body;
        }
    }
    
    /** Assumes loops are perfectly nested
     * Checks if all innermost statements are valid
     *  currently, a valid statement is either an assignment statement or null
     * @param outerLoop
     * @return
     */
    public static boolean areAllInnermostStatementsValid(IASTForStatement outerLoop) {
        IASTNode body = outerLoop.getBody();
        if(body instanceof IASTCompoundStatement) {
            if(body.getChildren().length == 0) {
                return true;
            }
            //assuming perfect nesting, so only check children[0]
            else if(body.getChildren()[0] instanceof IASTForStatement) {
                return areAllInnermostStatementsValid((IASTForStatement) body.getChildren()[0]);
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
                       continue;
                    }
                    else if(child instanceof IASTNullStatement) {
                        continue;
                    }
                    else {
                        return false;
                    }
                }
                return true;
            }
        }
        else if(body instanceof CPPASTForStatement) {
            return areAllInnermostStatementsValid((CPPASTForStatement) body);
        }
        else { //neither compound nor for statement - body is the only statement
            if(body instanceof IASTBinaryExpression) {
                if(((IASTBinaryExpression) body).getOperator() == IASTBinaryExpression.op_assign) {
                    return true;
                }
            }
            //either not binary or not an assignment
            return false;
        }
    }    
    
    private ForLoopUtil() {
    }
}
