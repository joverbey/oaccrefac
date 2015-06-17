package edu.auburn.oaccrefac.internal.core.constprop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Determines the effect of a statement or expression on a constant environment.
 * <p>
 * Used by {@link ConstantPropagation}.
 * 
 * @author Jeff Overbey
 */
public class ConstPropNodeEvaluator {

    /** Result returned by {@link ConstPropNodeEvaluator#evaluate(IASTNode, ConstEnv)}. */
    public static final class Result {
        /** An environment where no variables are constant-valued */
        private static final Result EMPTY = new Result(ConstEnv.EMPTY, Collections.<IASTName, Long> emptyMap());

        /** The constant environment that results after the node has been evaluated. */
        public final ConstEnv environment;

        /** Maps constant-valued {@link IASTName} nodes under the node being evaluated to their constant values. */
        public final Map<IASTName, Long> constValuedNames;

        private Result(ConstEnv environment, Map<IASTName, Long> constValuedNames) {
            this.environment = environment;
            this.constValuedNames = constValuedNames;
        }
    }

    /**
     * Evaluates the effects of a statement, declaration, or expression on a constant environment.
     * <p>
     * For example, evaluating the statement <code>b = 2+a;</code> in the constant environment [a=1] results in the
     * constant environment [a=1,b=3].
     * 
     * @see Result
     */
    public static Result evaluate(IASTNode node, ConstEnv initialEnv) {
        ConstPropNodeEvaluator e = new ConstPropNodeEvaluator(initialEnv);
        if (node instanceof IASTStatement) {
            e.evaluate((IASTStatement) node);
        } else if (node instanceof IASTDeclaration) {
            e.evaluate((IASTDeclaration) node);
        } else if (node instanceof IASTExpression) {
            e.evaluate((IASTExpression) node);
        } else {
            return Result.EMPTY;
        }
        return new Result(e.env, e.constValuedNames);
    }

    private ConstEnv env;

    private final Map<IASTName, Long> constValuedNames;

    private ConstPropNodeEvaluator(ConstEnv initialEnv) {
        this.env = initialEnv;
        this.constValuedNames = new HashMap<IASTName, Long>();
    }

    private void unhandled(IASTNode node) {
        System.err.println("Unhandled " + node + " - " + ASTUtil.toString(node));
        // Unsure how to handle this node
        // Conservatively assume that it could change anything -- nothing is constant-valued anymore
        env = ConstEnv.EMPTY;
    }

    private void evaluate(IASTStatement node) {
        if (node instanceof IASTDeclarationStatement) {
            evaluate(((IASTDeclarationStatement) node).getDeclaration());
        } else if (node instanceof IASTExpressionStatement) {
            evaluate(((IASTExpressionStatement) node).getExpression());
        } else {
            unhandled(node);
        }
    }

    private void evaluate(IASTDeclaration declaration) {
        if (!(declaration instanceof IASTSimpleDeclaration)) {
            unhandled(declaration);
            return;
        }

        IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) declaration;
        for (IASTDeclarator declarator : simpleDecl.getDeclarators()) {
            if (declarator.getNestedDeclarator() != null || declarator.getPointerOperators().length > 0) {
                unhandled(declaration);
                return;
            }

            if (declarator.getInitializer() != null) {
                if (!(declarator.getInitializer() instanceof IASTEqualsInitializer)) {
                    unhandled(declarator);
                    return;
                }

                IASTEqualsInitializer eqInit = (IASTEqualsInitializer) declarator.getInitializer();
                IASTInitializerClause initializer = eqInit.getInitializerClause();
                if (!(initializer instanceof IASTExpression)) {
                    unhandled(declaration);
                    return;
                }

                IASTName name = declarator.getName();
                Long value = evaluate((IASTExpression) initializer);
                if (env == null)
                    env = ConstEnv.EMPTY;
                IBinding binding = name.resolveBinding();
                if (ConstantPropagation.canTrackConstantValues(binding)
                        && ConstantPropagation.isInTrackedRange(binding, value)) {
                    env = env.set(binding, value);
                    constValuedNames.put(name, value);
                }
            }
        }
    }

    private Long evaluate(IASTExpression expression) {
        ExpressionEvaluator.Result result = ExpressionEvaluator.evaluate(expression, env);
        env = result.environment;
        constValuedNames.putAll(result.constValuedNames);
        return result.value;
    }
}
