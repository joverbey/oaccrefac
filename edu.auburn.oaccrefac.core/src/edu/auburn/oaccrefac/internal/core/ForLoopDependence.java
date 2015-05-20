package edu.auburn.oaccrefac.internal.core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;

/**
 * Container for perfectly nested for statements that are candidates 
 * for Fourier-Motzkin elimination
 *
 * 
 *
 */
public class ForLoopDependence {

    private Map<String, InductionVariable> inductionVariables;
    private Set<Pair<IndexExpression, IndexExpression>> equalities;

    public ForLoopDependence() {
        this.inductionVariables = new HashMap<String, InductionVariable>();
        this.equalities = new HashSet<Pair<IndexExpression, IndexExpression>>();
    }

    public void addForLoop(CPPASTForStatement statement) {
        findInductionVariables(statement);
        
        // check for imperfect loop nesting
        boolean hasForChild = false;
        boolean hasNonForChild = false;
        for (IASTNode child : statement.getBody().getChildren()) {
            if (hasForChild) {
                ASTUtil.raise("Imperfect loop nest", child);
            }

            if (child instanceof CPPASTForStatement) {
                if (hasNonForChild) {
                    ASTUtil.raise("Imperfect loop nest", child);
                }

                hasForChild = true;
                addForLoop((CPPASTForStatement) child);
            } else {
                hasNonForChild = true;
            }
        }

        // Only call for the deepest loop nest
        if (!hasForChild) {
            findEqualities(statement.getBody());
        }
    }

    public Matrix generateInequalities() {
        Matrix A = new Matrix();
        int index = 0;
        int numCols = inductionVariables.size() * 2 + 1;
        List<String> orderedVariables = new ArrayList<String>(inductionVariables.keySet());
        System.out.println("Order is " + orderedVariables);

        for (String name : orderedVariables) {
            double[] row = new double[numCols];
            // Index variable is less than the trip count
            row[index] = 1;
            row[numCols - 1] = inductionVariables.get(name).getTripCount();
            A.addRowAtIndex(A.getNumRows(), row);

            // Index variable is non-negative
            row[index] = -1;
            row[numCols - 1] = 0;
            A.addRowAtIndex(A.getNumRows(), row);

            index++;

            // Add the same rows but for the right side variable
            row = new double[numCols];
            row[index] = 1;
            row[numCols - 1] = inductionVariables.get(name).getTripCount();
            A.addRowAtIndex(A.getNumRows(), row);

            row[index] = -1;
            row[numCols - 1] = 0;
            A.addRowAtIndex(A.getNumRows(), row);

            index++;
        }

        // Add equalities to the matrix
        // Add 2 rows, one of the form Ax <= b and the other -Ax <= -b
        for (Pair<IndexExpression, IndexExpression> equality : equalities) {
            IndexExpression lhs = equality.getFirst();
            IndexExpression rhs = equality.getSecond();

            int b = rhs.getConstantFactor() - lhs.getConstantFactor();
            double[] row = new double[numCols];
            for (Entry<String, Integer> pair : lhs.getVariableFactors().entrySet()) {
                int column = orderedVariables.indexOf(pair.getKey()) * 2;
                row[column] = pair.getValue();
                b -= inductionVariables.get(pair.getKey()).getLowerBound();
            }

            for (Entry<String, Integer> pair : rhs.getVariableFactors().entrySet()) {
                int column = orderedVariables.indexOf(pair.getKey()) * 2 + 1;
                row[column] = -pair.getValue();
                b += inductionVariables.get(pair.getKey()).getLowerBound();
            }

            row[row.length - 1] = b;

            A.addRowAtIndex(A.getNumRows(), row);
        }

        return A;
    }

    private void findEqualities(IASTStatement body) {
        List<IASTBinaryExpression> binExprs = ASTUtil.find(body, IASTBinaryExpression.class);
        for (IASTBinaryExpression binExpr : binExprs) {
            int op = binExpr.getOperator();
            if (op != IASTBinaryExpression.op_assign) {
                // TODO: also handle +=, -=, etc.
                continue;
            }

            // TODO For now, only expressions where both sides are subscripts are
            // considered. In the future, it would probably be useful to also
            // handle transitive references (e.g. x = A[i]; ... A[i+1] = x;)
            if (!(binExpr.getOperand1() instanceof IASTArraySubscriptExpression)
                    || !(binExpr.getOperand2() instanceof IASTArraySubscriptExpression)) {
                continue;
            }

            IASTArraySubscriptExpression lhs = (IASTArraySubscriptExpression) binExpr.getOperand1();
            IASTArraySubscriptExpression rhs = (IASTArraySubscriptExpression) binExpr.getOperand2();
            IndexExpression leftIndex = indexExpressionForSubscript(lhs);
            IndexExpression rightIndex = indexExpressionForSubscript(rhs);
            equalities.add(new Pair<IndexExpression, IndexExpression>(leftIndex, rightIndex));
        }
    }

    private IndexExpression indexExpressionForSubscript(IASTArraySubscriptExpression expr) {
        if (!(expr.getArgument() instanceof IASTBinaryExpression)) {
            ASTUtil.raise("Array access is not a binary expression", expr.getArgument());
        }

        return new IndexParser().parseBinaryExpression((IASTBinaryExpression) expr.getArgument());
    }

    private void findInductionVariables(CPPASTForStatement statement) {
        String name;
        int initialValue = -1;

        IASTStatement initializerStatement = statement.getInitializerStatement();
        if (initializerStatement instanceof IASTDeclarationStatement) {
            // initializer is of the form "int i = 0"
            // TODO: Handle initializers with multiple declarations? e.g.
            // "for(int i = 0, j = 1, k = 2...)"
            IASTDeclaration declaration = ((IASTDeclarationStatement) initializerStatement).getDeclaration();
            IASTDeclarator declarator = ASTUtil.findOne(declaration, IASTDeclarator.class);
            name = declarator.getName().toString();

            if (!(declarator.getInitializer() instanceof IASTEqualsInitializer)) {
                ASTUtil.raise("Expected initializer to be IASTEqualsInitializer", declarator.getInitializer());
            }

            IASTEqualsInitializer initializer = (IASTEqualsInitializer) declarator.getInitializer();
            if (!(initializer.getInitializerClause() instanceof IASTLiteralExpression)) {
                ASTUtil.raise("Only IASTLiteralExpressions are supported in initializers",
                        initializer.getInitializerClause());
            }

            IASTLiteralExpression rhs = (IASTLiteralExpression) initializer.getInitializerClause();
            try {
                initialValue = Integer.parseInt(String.valueOf(rhs.getValue()));
            } catch (NumberFormatException ex) {
                ASTUtil.raise(
                        "Right hand side of initializer must be an integer but was not: "
                                + String.valueOf(rhs.getValue()), rhs);
            }
        } else if (initializerStatement instanceof IASTExpressionStatement) {
            // initializer is of the form "i = 0" (or some other expression, but
            // only assignment is supported)
            IASTExpressionStatement exprStatement = (IASTExpressionStatement) initializerStatement;
            if (!(exprStatement.getExpression() instanceof IASTBinaryExpression)) {
                ASTUtil.raise("Expected initializer expression to be IASTBinaryExpression",
                        exprStatement.getExpression());
            }

            IASTBinaryExpression binExpr = (IASTBinaryExpression) exprStatement.getExpression();
            if (!(binExpr.getOperand1() instanceof IASTIdExpression)
                    || !(binExpr.getOperand2() instanceof IASTLiteralExpression)) {
                ASTUtil.raise(
                        "Only binary expressions where the left side is an ID and the right side is a literal are supported",
                        binExpr);
            }
            if (binExpr.getOperator() != IASTBinaryExpression.op_assign) {
                ASTUtil.raise("Expected initializer to be an assignment statement", binExpr);
            }

            IASTIdExpression lhs = (IASTIdExpression) binExpr.getOperand1();
            IASTLiteralExpression rhs = (IASTLiteralExpression) binExpr.getOperand2();
            name = lhs.getName().toString();
            try {
                initialValue = Integer.parseInt(String.valueOf(rhs.getValue()));
            } catch (NumberFormatException ex) {
                ASTUtil.raise("Right side of initializer must be an integer literal", rhs);
            }
        } else {
            throw new IllegalArgumentException(
                    "Expected initializer to be IASTDeclarationStatement or IASTExpressionStatement, not "
                            + initializerStatement.getClass().getName());
        }

        // Find upper/lower bound from conditional
        IASTExpression conditional = statement.getConditionExpression();
        if (!(conditional instanceof IASTBinaryExpression)) {
            ASTUtil.raise("Only binary expressions are supported for conditionals", conditional);
        }

        IASTBinaryExpression binExpr = (IASTBinaryExpression) conditional;
        if (!(binExpr.getOperand1() instanceof IASTIdExpression)
                || !(binExpr.getOperand2() instanceof IASTLiteralExpression)) {
            ASTUtil.raise(
                    "Only binary expressions where the left side is an ID and the right side is a literal are supported",
                    binExpr);
        }

        int op = binExpr.getOperator();
        if (op != IASTBinaryExpression.op_lessThan && op != IASTBinaryExpression.op_lessEqual
                && op != IASTBinaryExpression.op_greaterThan && op != IASTBinaryExpression.op_greaterEqual) {
            ASTUtil.raise("Only the following operators are supported in conditionals: < > <= >=", binExpr);
        }

        IASTIdExpression lhs = (IASTIdExpression) binExpr.getOperand1();
        IASTLiteralExpression rhs = (IASTLiteralExpression) binExpr.getOperand2();
        if (!lhs.getName().toString().equals(name)) {
            ASTUtil.raise("Expected conditional to be a restriction on induction variable " + name, lhs);
        }

        try {
            int finalValue = Integer.parseInt(String.valueOf(rhs.getValue()));
            if (op == IASTBinaryExpression.op_lessThan)
                finalValue--;
            if (op == IASTBinaryExpression.op_greaterThan)
                finalValue++;

            InductionVariable variable = new InductionVariable(name, initialValue, finalValue);
            inductionVariables.put(name, variable);
            System.out.println(inductionVariables);
        } catch (NumberFormatException ex) {
            ASTUtil.raise("Literal on the right side of the conditional must be an integer", rhs);
        }
    }
}
