package edu.auburn.oaccrefac.core.dependence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
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
import org.eclipse.core.runtime.IProgressMonitor;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.BindingComparator;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;
import edu.auburn.oaccrefac.internal.core.Pair;
import edu.auburn.oaccrefac.internal.core.dependence.LinearExpression;
import edu.auburn.oaccrefac.internal.core.dependence.VariableAccess;

public abstract class AbstractDependenceAnalysis {

    private final List<VariableAccess> variableAccesses;
    private final Set<DataDependence> dependences;
    
    public AbstractDependenceAnalysis(IProgressMonitor pm, IASTStatement... statements) throws DependenceTestFailure {
        this.variableAccesses = new ArrayList<VariableAccess>();
        this.dependences = new HashSet<DataDependence>();
        
        pm.subTask("Finding variable accesses...");
        collectAccessesFromStatements(statements);
        
    }
    
    protected Set<IBinding> collectAllVariables(LinearExpression[]... exprs) {
        Set<IBinding> vars = new TreeSet<IBinding>(new BindingComparator());
        for (LinearExpression[] array : exprs) {
            for (LinearExpression e : array) {
                vars.addAll(e.getCoefficients().keySet());
            }
        }
        return vars;
    }

    protected abstract void computeDependences(IProgressMonitor pm) throws DependenceTestFailure;
    
    protected int[] fillArray(int length, int value) {
        int[] result = new int[length];
        Arrays.fill(result, value);
        return result;
    }

    protected boolean feasibleControlFlow(VariableAccess v1, VariableAccess v2) {
        return v1.isInCommonLoopsWith(v2) || v1.enclosingStatementLexicallyPrecedes(v2);
    }

    protected void collectAccessesFromStatements(IASTStatement... statements) throws DependenceTestFailure {
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

    protected void collectAccessesFrom(IASTNullStatement stmt) {
        // Nothing to do
    }

    protected void collectAccessesFrom(IASTCompoundStatement stmt) throws DependenceTestFailure {
        collectAccessesFromStatements(stmt.getStatements());
    }

    protected void collectAccessesFrom(IASTForStatement stmt) throws DependenceTestFailure {
        ForStatementInquisitor forLoop = InquisitorFactory.getInquisitor(stmt);
        if (!forLoop.isCountedLoop()) {
            throw unsupported(stmt);
        }

        collectAccessesFromStatements(stmt.getBody());
    }

    protected void collectAccessesFrom(IASTDeclarationStatement stmt) throws DependenceTestFailure {
        if (!(stmt.getDeclaration() instanceof IASTSimpleDeclaration))
            throw unsupported(stmt);

        IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) stmt.getDeclaration();
        for (IASTDeclarator declarator : simpleDecl.getDeclarators()) {
            if (declarator.getNestedDeclarator() != null || declarator.getPointerOperators().length > 0)
                throw unsupported(stmt);

            variableAccesses.add(new VariableAccess(true, declarator.getName()));

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

    protected void collectAccessesFrom(IASTExpressionStatement stmt) throws DependenceTestFailure {
        Pair<IASTExpression, IASTExpression> asgt = ASTUtil.getAssignment(stmt.getExpression());
        if (asgt == null)
            throw unsupported(stmt);

        collectAccessesFromAssignmentLHS(asgt.getFirst());
        collectAccessesFromExpression(asgt.getSecond());
    }

    protected void collectAccessesFromAssignmentLHS(IASTExpression expr) throws DependenceTestFailure {
        IASTName scalar = ASTUtil.getIdExpression(expr);
        if (scalar != null) {
            variableAccesses.add(new VariableAccess(true, scalar));
            return;
        }

        Pair<IASTName, IASTName> fieldReference = ASTUtil.getSimpleFieldReference(expr);
        if (fieldReference != null) {
            IASTName owner = fieldReference.getFirst();
            IASTName field = fieldReference.getSecond();
            variableAccesses.add(new VariableAccess(false, owner));
            variableAccesses.add(new VariableAccess(true, field));
            return;
        }

        Pair<IASTName, LinearExpression[]> arrayAccess = ASTUtil.getMultidimArrayAccess(expr);
        if (arrayAccess != null) {
            variableAccesses.add(new VariableAccess(true, arrayAccess.getFirst(), arrayAccess.getSecond()));
            return;
        }

        throw unsupported(expr);
    }

    protected void collectAccessesFromExpression(IASTExpression expr) throws DependenceTestFailure {
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
        } else if (expr instanceof IASTFieldReference) {
            collectAccessesFrom((IASTFieldReference) expr);
        } else {
            throw unsupported(expr);
        }
    }

    protected void collectAccessesFrom(IASTBinaryExpression expr) throws DependenceTestFailure {
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

    protected void collectAccessesFrom(IASTUnaryExpression expr) throws DependenceTestFailure {
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

    protected void collectAccessesFrom(IASTLiteralExpression expr) {
        // Nothing to do
    }

    protected void collectAccessesFrom(IASTIdExpression expr) throws DependenceTestFailure {
        IASTName name = ASTUtil.getIdExpression(expr);
        if (name == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(false, name));
    }

    protected void collectAccessesFrom(IASTFieldReference expr) throws DependenceTestFailure {
        Pair<IASTName, IASTName> pair = ASTUtil.getSimpleFieldReference(expr);
        if (pair == null)
            throw unsupported(expr);

        IASTName owner = pair.getFirst();
        IASTName field = pair.getSecond();
        variableAccesses.add(new VariableAccess(false, owner));
        variableAccesses.add(new VariableAccess(false, field));
    }

    protected void collectAccessesFrom(IASTArraySubscriptExpression expr) throws DependenceTestFailure {
        Pair<IASTName, LinearExpression[]> arrayAccess = ASTUtil.getMultidimArrayAccess(expr);
        if (arrayAccess == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(false, arrayAccess.getFirst(), arrayAccess.getSecond()));

        collectAccessesFromExpression((IASTExpression) expr.getArgument());
    }

    protected static DependenceTestFailure unsupported(IASTNode node) {
        return new DependenceTestFailure(
                String.format("Unsupported construct on line %d (%s)", node.getFileLocation().getStartingLineNumber(), //
                        node.getClass().getSimpleName()));
    }

    public Set<DataDependence> getDependences() {
        return Collections.unmodifiableSet(dependences);
    }

    public boolean hasLevel1CarriedDependence() {
        for (DataDependence dep : dependences) {
            if (dep.getLevel() == 1) {
                return true;
            }
        }
        return false;
    }
    
    protected void addDependence(DataDependence dependence) {
        dependences.add(dependence);
    }

    protected List<VariableAccess> getVariableAccesses() {
        return variableAccesses;
    }
}
