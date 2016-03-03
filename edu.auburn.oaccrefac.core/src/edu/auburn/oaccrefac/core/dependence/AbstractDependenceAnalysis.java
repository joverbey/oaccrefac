/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
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
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
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
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.BindingComparator;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;
import edu.auburn.oaccrefac.internal.core.Pair;
import edu.auburn.oaccrefac.internal.core.dependence.FunctionWhitelist;
import edu.auburn.oaccrefac.internal.core.dependence.LinearExpression;
import edu.auburn.oaccrefac.internal.core.dependence.VariableAccess;

/**
 * AbstractDependenceAnalysis serves as the base of any dependence
 * analysis.
 * 
 * @author Jeff Overbey
 * @author Adam Eichelkraut
 */
public abstract class AbstractDependenceAnalysis {

    private final List<VariableAccess> variableAccesses;

    private final Set<DataDependence> dependences;

    /**
     * Analyzes dependences in a sequence of C statements.
     * 
     * @throws DependenceTestFailure
     */
    public AbstractDependenceAnalysis(IProgressMonitor pm, IASTStatement... statements)
            throws DependenceTestFailure, OperationCanceledException {
        this.variableAccesses = new ArrayList<VariableAccess>();
        this.dependences = new HashSet<DataDependence>();

        pm.subTask("Finding variable accesses...");
        collectAccessesFromStatements(statements);
    }

    public List<VariableAccess> getVariableAccesses() {
        return this.variableAccesses;
    }

    protected void addDependence(DataDependence dataDependence) {
        this.dependences.add(dataDependence);
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

    protected int[] fillArray(int length, int value) {
        int[] result = new int[length];
        Arrays.fill(result, value);
        return result;
    }

    protected boolean feasibleControlFlow(VariableAccess v1, VariableAccess v2) {
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
        ForStatementInquisitor forLoop = InquisitorFactory.getInquisitor(stmt);
        if (!forLoop.isCountedLoop()) {
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

    private void collectAccessesFrom(IASTExpressionStatement stmt) throws DependenceTestFailure {
        Pair<IASTExpression, IASTExpression> asgt = ASTUtil.getAssignment(stmt.getExpression());
        if (asgt != null) {
            collectAccessesFromAssignmentLHS(asgt.getFirst());
            collectAccessesFromExpression(asgt.getSecond());
            return;
        }

        Pair<IASTExpression, IASTExpression> assignEq = ASTUtil.getAssignEq(stmt.getExpression());
        if (assignEq != null) {
            // The variable on the LHS is both read and written
            collectAccessesFromAssignmentLHS(assignEq.getFirst());
            collectAccessesFromExpression(assignEq.getFirst());
            collectAccessesFromExpression(assignEq.getSecond());
            return;
        }

        IASTExpression incrDecr = ASTUtil.getIncrDecr(stmt.getExpression());
        if (incrDecr != null) {
            // The incremented variable/array element is both read and written
            collectAccessesFromAssignmentLHS(incrDecr);
            collectAccessesFromExpression(incrDecr);
            return;
        }

        throw unsupported(stmt);
    }

    private void collectAccessesFromAssignmentLHS(IASTExpression expr) throws DependenceTestFailure {
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
        } else if (expr instanceof IASTFieldReference) {
            collectAccessesFrom((IASTFieldReference) expr);
        } else if (expr instanceof IASTFunctionCallExpression) {
            collectAccessesFrom((IASTFunctionCallExpression) expr);
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
        variableAccesses.add(new VariableAccess(false, name));
    }

    private void collectAccessesFrom(IASTFieldReference expr) throws DependenceTestFailure {
        Pair<IASTName, IASTName> pair = ASTUtil.getSimpleFieldReference(expr);
        if (pair == null)
            throw unsupported(expr);

        IASTName owner = pair.getFirst();
        IASTName field = pair.getSecond();
        variableAccesses.add(new VariableAccess(false, owner));
        variableAccesses.add(new VariableAccess(false, field));
    }

    private void collectAccessesFrom(IASTFunctionCallExpression expr) throws DependenceTestFailure {
        if (FunctionWhitelist.isWhitelisted(expr)) {
            // Whitelisted functions are known not to affect any aliased variables
            for (IASTInitializerClause arg : expr.getArguments()) {
                if (arg instanceof IASTExpression) {
                    collectAccessesFromExpression((IASTExpression)arg);
                } else {
                    throw unsupported(arg);
                }
            }
        } else {
            IASTFunctionDefinition function = ASTUtil.findNearestAncestor(expr, IASTFunctionDefinition.class);
            // Conservatively assume that a function call might change every variable
            // whose address is taken (via pointers)...
            AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, new NullProgressMonitor());
            for (IVariable var : analysis.getAddressTakenVariables()) {
                variableAccesses.add(new VariableAccess(false, expr, var)); // Read
                variableAccesses.add(new VariableAccess(true, expr, var));  // Write
            }
            // ...but otherwise it will not change any local variables
        }
    }

    private void collectAccessesFrom(IASTArraySubscriptExpression expr) throws DependenceTestFailure {
        Pair<IASTName, LinearExpression[]> arrayAccess = ASTUtil.getMultidimArrayAccess(expr);
        if (arrayAccess == null)
            throw unsupported(expr);
        variableAccesses.add(new VariableAccess(false, arrayAccess.getFirst(), arrayAccess.getSecond()));

        collectAccessesFromExpression((IASTExpression) expr.getArgument());
    }

    private static DependenceTestFailure unsupported(IASTNode node) {
        return new DependenceTestFailure(String.format("Unsupported construct on line %d (%s) - %s",
                node.getFileLocation().getStartingLineNumber(), //
                node.getClass().getSimpleName(), ASTUtil.toString(node)));
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

    protected abstract void computeDependences(IProgressMonitor pm) throws DependenceTestFailure;
}
