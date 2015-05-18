package edu.auburn.oaccrefac.internal.core;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;

public class IndexParser {
    private IndexExpression index;

    public IndexParser() {
        index = new IndexExpression();
    }

    public IndexExpression getIndexExpression() {
        return index;
    }

    public IndexExpression parseBinaryExpression(IASTBinaryExpression binExpr) {
        parseBinaryExpression(binExpr, 1);
        return index;
    }

    private void parseBinaryExpression(IASTBinaryExpression binExpr, int negationFlag) {
        // negationFlag is used when recursing to indicate whether binExpr is a
        // descendant of the right side of a subtraction expression
        // TODO: also handle unary negative
        IASTNode lhs = binExpr.getOperand1();
        IASTNode rhs = binExpr.getOperand2();

        if (lhs instanceof IASTLiteralExpression && rhs instanceof IASTLiteralExpression) {
            // literal <op> literal -> raise exception because we don't support
            // constant folding
            ASTUtil.raise("Binary expression where both sides are literals should be reduced to a single literal",
                    binExpr);
        } else if (lhs instanceof IASTBinaryExpression && rhs instanceof IASTBinaryExpression) {
            // binexp <op> binexp
            handleBothBinaryExpressions(binExpr, negationFlag);
        } else if (lhs instanceof IASTBinaryExpression && rhs instanceof IASTIdExpression
                || lhs instanceof IASTIdExpression && rhs instanceof IASTBinaryExpression) {
            // binexp <op> id or id <op> binexp
            handleBinaryIdExpression(binExpr, negationFlag);
        } else if (lhs instanceof IASTBinaryExpression && rhs instanceof IASTLiteralExpression
                || lhs instanceof IASTLiteralExpression && rhs instanceof IASTBinaryExpression) {
            // binexp <op> literal or literal <op> binexp
            handleBinaryLiteralExpression(binExpr, negationFlag);
        } else if (lhs instanceof IASTIdExpression && rhs instanceof IASTLiteralExpression
                || lhs instanceof IASTLiteralExpression && rhs instanceof IASTIdExpression) {
            // literal <op> id or id <op> literal
            handleIdLiteralExpression(binExpr, negationFlag);
        } else if (lhs instanceof IASTIdExpression && rhs instanceof IASTIdExpression) {
            // id <op> id
            handleBothIdExpressions(binExpr, negationFlag);
        } else {
            ASTUtil.raise("Invalid BinaryExpression", binExpr);
        }
    }

    private void handleBothIdExpressions(IASTBinaryExpression binExpr, int negationFlag) {
        IASTIdExpression lhs = (IASTIdExpression) binExpr.getOperand1();
        IASTIdExpression rhs = (IASTIdExpression) binExpr.getOperand2();
        int sign = 1;

        switch (binExpr.getOperator()) {
        case IASTBinaryExpression.op_minus:
            sign = -1;
        case IASTBinaryExpression.op_plus:
            String lName = lhs.getName().toString();
            index.addVariable(lName, 1 * negationFlag);
            String rName = rhs.getName().toString();
            index.addVariable(rName, 1 * sign * negationFlag);
            break;
        default:
            ASTUtil.raise("Index expression is not affine", binExpr);
            break;
        }
    }

    private void handleBinaryLiteralExpression(IASTBinaryExpression binExpr, int negationFlag) {
        IASTNode lhs = binExpr.getOperand1();
        IASTNode rhs = binExpr.getOperand2();
        int sign = 1;

        switch (binExpr.getOperator()) {
        case IASTBinaryExpression.op_minus:
            sign = -1;
        case IASTBinaryExpression.op_plus:
            if (lhs instanceof IASTBinaryExpression && rhs instanceof IASTLiteralExpression) {
                parseBinaryExpression((IASTBinaryExpression) lhs, negationFlag);
                int constVal = intValueOfLiteral((IASTLiteralExpression) rhs);
                index.addConstantFactor(constVal * sign * negationFlag);
            } else {
                // Left side is the literal, right side is the binaryexpression
                int constVal = intValueOfLiteral((IASTLiteralExpression) lhs);
                index.addConstantFactor(constVal * negationFlag);
                parseBinaryExpression((IASTBinaryExpression) rhs, sign * negationFlag);
            }
            break;
        default:
            ASTUtil.raise("Invalid binary expression", binExpr);
            break;
        }
    }

    private void handleIdLiteralExpression(IASTBinaryExpression binExpr, int negationFlag) {
        IASTNode lhs = binExpr.getOperand1();
        IASTNode rhs = binExpr.getOperand2();
        int sign = 1;

        switch (binExpr.getOperator()) {
        case IASTBinaryExpression.op_minus:
            sign = -1;
        case IASTBinaryExpression.op_plus:
            if (lhs instanceof IASTIdExpression && rhs instanceof IASTLiteralExpression) {
                String name = ((IASTIdExpression) lhs).getName().toString();
                index.addVariable(name, 1 * negationFlag);
                int constVal = intValueOfLiteral((IASTLiteralExpression) rhs);
                index.addConstantFactor(constVal * sign * negationFlag);
            } else {
                // Left side is the literal, right side is the id
                int constVal = intValueOfLiteral((IASTLiteralExpression) lhs);
                index.addConstantFactor(constVal * negationFlag);
                String name = ((IASTIdExpression) rhs).getName().toString();
                index.addVariable(name, 1 * sign * negationFlag);
            }
            break;
        case IASTBinaryExpression.op_multiply:
            if (lhs instanceof IASTIdExpression && rhs instanceof IASTLiteralExpression) {
                String name = ((IASTIdExpression) lhs).getName().toString();
                int constVal = intValueOfLiteral((IASTLiteralExpression) rhs);
                index.addVariable(name, constVal * negationFlag);
            } else {
                // Left side is the literal, right side is the id
                int constVal = intValueOfLiteral((IASTLiteralExpression) lhs);
                String name = ((IASTIdExpression) rhs).getName().toString();
                index.addVariable(name, constVal * negationFlag);
            }
            break;
        default:
            ASTUtil.raise("Invalid binary expression", binExpr);
            break;
        }
    }

    private void handleBinaryIdExpression(IASTBinaryExpression binExpr, int negationFlag) {
        IASTNode lhs = binExpr.getOperand1();
        IASTNode rhs = binExpr.getOperand2();
        int sign = 1;

        switch (binExpr.getOperator()) {
        case IASTBinaryExpression.op_minus:
            sign = -1;
        case IASTBinaryExpression.op_plus:
            if (lhs instanceof IASTIdExpression && rhs instanceof IASTBinaryExpression) {
                String name = ((IASTIdExpression) lhs).getName().toString();
                index.addVariable(name, 1 * negationFlag);
                parseBinaryExpression((IASTBinaryExpression) rhs, sign * negationFlag);
            } else {
                // Left side is the binary expression, right side is the id
                parseBinaryExpression((IASTBinaryExpression) rhs, negationFlag);
                String name = ((IASTIdExpression) rhs).getName().toString();
                index.addVariable(name, 1 * sign * negationFlag);
            }
            break;
        default:
            ASTUtil.raise("Invalid binary expression", binExpr);
            break;
        }
    }

    private void handleBothBinaryExpressions(IASTBinaryExpression binExpr, int negationFlag) {
        IASTBinaryExpression lhs = (IASTBinaryExpression) binExpr.getOperand1();
        IASTBinaryExpression rhs = (IASTBinaryExpression) binExpr.getOperand2();
        int sign = 1;

        switch (binExpr.getOperator()) {
        case IASTBinaryExpression.op_minus:
            sign = -1;
        case IASTBinaryExpression.op_plus:
            parseBinaryExpression(lhs, negationFlag);
            parseBinaryExpression(rhs, sign * negationFlag);
            break;
        default:
            ASTUtil.raise("Invalid binary expression", binExpr);
            break;
        }
    }

    private int intValueOfLiteral(IASTLiteralExpression literal) {
        try {
            return Integer.parseInt(String.valueOf(((IASTLiteralExpression) literal).getValue()));
        } catch (NumberFormatException ex) {
            ASTUtil.raise("Only integer literals are allowed", literal);
        }

        // Will never happen
        return 0;
    }
}
