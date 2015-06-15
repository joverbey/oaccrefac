package edu.auburn.oaccrefac.internal.core;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryIntegerConstant;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

public class ForLoopUtil {

    // Patterns of for loops that are acceptable to refactor...
    private static final String[] patterns = { //
            "for (i = 0; i < 1; i++) ;", //
            "for (int i = 0; i < 1; i++) ;", //
            "for (i = 0; i <= 1; i++) ;", //
            "for (int i = 0; i <= 1; i++) ;", //
            "for (i = 0; i < 1; i+=1) ;", //

            "for (int i = 0; i < 1; i+=1) ;", //
            "for (i = 0; i <= 1; i+=1) ;", //
            "for (int i = 0; i <= 1; i+=1) ;", //

            "for (i = 0; i < 1; i=i+1) ;", //
            "for (int i = 0; i < 1; i=i+1) ;", //
            "for (i = 0; i <= 1; i=i+1) ;", //
            "for (int i = 0; i <= 1; i=i+1) ;", };

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

    private ForLoopUtil() {
    }
}
