package edu.auburn.oaccrefac.core.newtmp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.BindingComparator;
import edu.auburn.oaccrefac.internal.core.ForLoopUtil;
import edu.auburn.oaccrefac.internal.core.Pair;
import edu.auburn.oaccrefac.internal.core.fromphotran.DependenceTestFailure;

/**
 * Analyzes data dependences between statements.
 * 
 * @author Alexander Calvert
 * @author Jeff Overbey
 */
public class DependenceAnalysis {

    private final List<VariableAccess> variableAccesses = new ArrayList<VariableAccess>();
    
    private final FourierMotzkinDependenceTest tester = new FourierMotzkinDependenceTest();

    /**
     * Constructor takes a for statement in, setting up this instance's dependence dependence system of equations if it
     * is valid
     * 
     * @param outerLoop
     *            the outer for loop to be
     * @throws DependenceTestFailure
     */
    public Set<DataDependence> analyze(IASTStatement... statements) throws DependenceTestFailure {
        collectAccessesFromStatements(statements);

        Set<DataDependence> dependences = new HashSet<DataDependence>();
        for (VariableAccess v1 : variableAccesses) {
            for (VariableAccess v2 : variableAccesses) {
                if (v1.refersToSameVariableAs(v2) && (v1.isWrite() || v2.isWrite()) && feasibleControlFlow(v1, v2)) {
                    IASTStatement s1 = v1.getEnclosingStatement();
                    IASTStatement s2 = v2.getEnclosingStatement();
                    DependenceType dependenceType = DependenceType.forAccesses(v1, v2);
                    if (v1.isScalarAccess() || v2.isScalarAccess()) {
                        dependences.add(new DataDependence(s1, s2, new DirectionVector(v1.numEnclosingLoops()), dependenceType));
                    } else {
                        // FIXME Handle loop nests properly -- bounds, index variables, etc.
                        //List<IASTForStatement> commonLoops = v1.getCommonEnclosingLoops(v2);
                        IBinding[] vars = collectAllVariables(v1.getLinearSubscriptExpression(), v2.getLinearSubscriptExpression());
                        int[][] writeCoefficients = new int[][] { v1.collectCoefficients(vars) };
                        int[][] readCoefficients = new int[][] { v2.collectCoefficients(vars) };
                        int[] lowerBounds = fillArray(vars.length, Integer.MIN_VALUE);
                        int[] upperBounds = fillArray(vars.length, Integer.MAX_VALUE);
                        DirectionVector direction = new DirectionVector(vars.length);
                        if (tester.test(lowerBounds, upperBounds, writeCoefficients, readCoefficients, direction.getElements())) {
                            dependences.add(new DataDependence(s1, s2, direction, dependenceType));
                        }
                    }
                }
            }
        }
        return dependences;
    }

    private IBinding[] collectAllVariables(LinearExpression le1, LinearExpression le2) {
        Set<IBinding> vars = new HashSet<IBinding>();
        vars.addAll(le1.getCoefficients().keySet());
        vars.addAll(le2.getCoefficients().keySet());
        IBinding[] result = vars.toArray(new IBinding[vars.size()]);
        Arrays.sort(result, new BindingComparator());
        return result;
    }

    private int[] fillArray(int length, int value) {
        int[] result = new int[length];
        Arrays.fill(result, value);
        return result;
    }

    private boolean feasibleControlFlow(VariableAccess v1, VariableAccess v2) {
        return v1.isInCommonLoopsWith(v2) || v1.enclosingStatementLexicallyPrecedes(v2);
    }

    private void collectAccessesFromStatements(IASTStatement... statements) throws DependenceTestFailure {
        for (IASTStatement stmt : statements) {
            if (stmt instanceof IASTDeclarationStatement) {
                collectAccessesFrom((IASTDeclarationStatement) stmt);
            } else if (stmt instanceof IASTForStatement) {
                collectAccessesFrom((IASTForStatement) stmt);
            } else if (stmt instanceof IASTExpressionStatement) {
                collectAccessesFrom((IASTExpressionStatement) stmt);
            } else if (stmt instanceof IASTNullStatement) {
                collectAccessesFrom((IASTNullStatement) stmt);
            } else if (stmt instanceof IASTCompoundStatement) {
                collectAccessesFrom((IASTCompoundStatement) stmt);
            } else {
                throw unsupported(stmt);
            }
        }
    }

    private void collectAccessesFrom(IASTNullStatement stmt) {
        // Nothing to do
    }

    private void collectAccessesFrom(IASTCompoundStatement stmt) throws DependenceTestFailure {
        collectAccessesFromStatements(stmt.getStatements());
    }

    private void collectAccessesFrom(IASTForStatement stmt) throws DependenceTestFailure {
        if (!ForLoopUtil.isCountedLoop(stmt)) {
            throw unsupported(stmt);
        }

        collectAccessesFromStatements(stmt.getBody());
    }

    private void collectAccessesFrom(IASTDeclarationStatement stmt) throws DependenceTestFailure {
        if (!(stmt.getDeclaration() instanceof IASTSimpleDeclaration))
            throw unsupported(stmt);

        IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) stmt.getDeclaration();
        for (IASTDeclarator declarator : simpleDecl.getDeclarators()) {
            if (declarator.getNestedDeclarator() != null || declarator.getPointerOperators().length > 0)
                throw unsupported(stmt);

            variableAccesses.add(new VariableAccess(declarator.getName(), true));

            if (declarator.getInitializer() != null) {
                if (!(declarator.getInitializer() instanceof IASTEqualsInitializer))
                    throw unsupported(stmt);
                IASTEqualsInitializer eqInit = (IASTEqualsInitializer) declarator.getInitializer();
                IASTInitializerClause initializer = eqInit.getInitializerClause();
                if (!(initializer instanceof IASTExpression))
                    throw unsupported(stmt);
                collectAccessesFromExpression((IASTExpression) initializer);
            }
        }
    }

    private void collectAccessesFrom(IASTExpressionStatement stmt) throws DependenceTestFailure {
        Pair<IASTExpression, IASTExpression> asgt = ASTUtil.getAssignment(stmt.getExpression());
        if (asgt == null)
            throw unsupported(stmt);

        collectAccessesFromAssignmentLHS(asgt.getFirst());
        collectAccessesFromExpression(asgt.getSecond());
    }

    private void collectAccessesFromAssignmentLHS(IASTExpression expr) throws DependenceTestFailure {
        IASTName scalar = ASTUtil.getIdExpression(expr);
        if (scalar != null) {
            variableAccesses.add(new VariableAccess(scalar, true));
            return;
        }

        Pair<IASTName, IASTExpression> arrayAccess = ASTUtil.getSupportedArrayAccess(expr);
        if (arrayAccess != null) {
            variableAccesses.add(new VariableAccess(arrayAccess, true));
            return;
        }

        throw unsupported(expr);
    }

    private void collectAccessesFromExpression(IASTExpression expr) throws DependenceTestFailure {
        if (expr instanceof IASTBinaryExpression) {
            collectAccessesFrom((IASTBinaryExpression) expr);
        } else if (expr instanceof IASTUnaryExpression) {
            collectAccessesFrom((IASTUnaryExpression) expr);
        } else if (expr instanceof IASTLiteralExpression) {
            collectAccessesFrom((IASTLiteralExpression) expr);
        } else if (expr instanceof IASTIdExpression) {
            collectAccessesFrom((IASTIdExpression) expr);
        } else if (expr instanceof IASTArraySubscriptExpression) {
            collectAccessesFrom((IASTArraySubscriptExpression) expr);
        } else {
            throw unsupported(expr);
        }
    }

    private void collectAccessesFrom(IASTBinaryExpression expr) throws DependenceTestFailure {
        switch (expr.getOperator()) {
        case IASTBinaryExpression.op_plus:
        case IASTBinaryExpression.op_minus:
        case IASTBinaryExpression.op_multiply:
        case IASTBinaryExpression.op_divide:
        case IASTBinaryExpression.op_modulo:
        case IASTBinaryExpression.op_binaryAnd:
        case IASTBinaryExpression.op_binaryOr:
        case IASTBinaryExpression.op_binaryXor:
        case IASTBinaryExpression.op_shiftLeft:
        case IASTBinaryExpression.op_shiftRight:
        case IASTBinaryExpression.op_greaterThan:
        case IASTBinaryExpression.op_greaterEqual:
        case IASTBinaryExpression.op_lessThan:
        case IASTBinaryExpression.op_lessEqual:
        case IASTBinaryExpression.op_equals:
        case IASTBinaryExpression.op_notequals:
            collectAccessesFromExpression(expr.getOperand1());
            collectAccessesFromExpression(expr.getOperand2());
            break;
        default:
            throw unsupported(expr);
        }
    }

    private void collectAccessesFrom(IASTUnaryExpression expr) throws DependenceTestFailure {
        switch (expr.getOperator()) {
        case IASTUnaryExpression.op_bracketedPrimary:
        case IASTUnaryExpression.op_plus:
        case IASTUnaryExpression.op_minus:
        case IASTUnaryExpression.op_not:
        case IASTUnaryExpression.op_tilde:
            collectAccessesFromExpression(expr.getOperand());
            break;
        default:
            throw unsupported(expr);
        }
    }

    private void collectAccessesFrom(IASTLiteralExpression expr) {
        // Nothing to do
    }

    private void collectAccessesFrom(IASTIdExpression expr) throws DependenceTestFailure {
        IASTName name = ASTUtil.getIdExpression(expr);
        if (name == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(name, false));
    }

    private void collectAccessesFrom(IASTArraySubscriptExpression expr) throws DependenceTestFailure {
        Pair<IASTName, IASTExpression> arrayAccess = ASTUtil.getSupportedArrayAccess(expr);
        if (arrayAccess == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(arrayAccess, true));

        collectAccessesFromExpression(arrayAccess.getSecond());
    }

    private static DependenceTestFailure unsupported(IASTNode node) {
        return new DependenceTestFailure(
                String.format("Unsupported construct on line %d (%s)", node.getFileLocation().getStartingLineNumber(), //
                        node.getClass().getSimpleName()));
    }
}
