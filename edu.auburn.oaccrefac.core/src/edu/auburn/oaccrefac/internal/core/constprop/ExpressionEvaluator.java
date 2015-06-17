/*******************************************************************************
 * Copyright (c) 2004, 2008, 2014 IBM Corporation, Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *     Markus Schorn - initial API and implementation
 *     Sergey Prigogin (Google)
 *     Markus Schorn (Wind River Systems)
 *     Bryan Wilkinson (QNX) - https://bugs.eclipse.org/bugs/show_bug.cgi?id=151207
 *     Anton Leherbauer (Wind River Systems)
 *     Jeff Overbey (Auburn) - copied/modified for constant propagation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.core.constprop;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeConstructorExpression;
import org.eclipse.cdt.core.parser.IProblem;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Evaluates a C expression in a given environment.
 * <p>
 * Used by {@link ConstantPropagation}.
 * <p>
 * Primarily based on org.eclipse.cdt.internal.core.dom.parser.Value with portions (getNumber) based on
 * org.eclipse.cdt.internal.core.parser.scanner.ExpressionEvaluator
 */
public class ExpressionEvaluator {

    public static final class Result {
        public final Long value;
        public final ConstEnv environment;
        public final Map<IASTName, Long> constValuedNames;

        private Result(Long value, ConstEnv environment, Map<IASTName, Long> constValuedNames) {
            this.value = value;
            this.environment = environment;
            this.constValuedNames = constValuedNames;
        }
    }

    public static Result evaluate(IASTExpression exp, ConstEnv initialEnv) {
        ExpressionEvaluator e = new ExpressionEvaluator(initialEnv);
        Long value = e.evaluate(exp);
        return new Result(value, e.env, e.constValuedNames);
    }

    private ConstEnv env;

    private Map<IASTName, Long> constValuedNames;

    private ExpressionEvaluator(ConstEnv initialEnv) {
        this.env = initialEnv;
        this.constValuedNames = new HashMap<IASTName, Long>();
    }

    /**
     * Computes the canonical representation of the value of the expression. Returns a {@code Number} for numerical
     * values or {@code null}, otherwise.
     * 
     * @throws UnknownValueException
     */
    private Long evaluate(IASTExpression exp) {
        if (exp == null)
            return null;

        if (exp instanceof IASTArraySubscriptExpression) {
            return null;
        }
        if (exp instanceof IASTBinaryExpression) {
            return evaluateBinaryExpression((IASTBinaryExpression) exp);
        }
        if (exp instanceof IASTCastExpression) { // must be ahead of unary
            return evaluate(((IASTCastExpression) exp).getOperand());
        }
        if (exp instanceof IASTUnaryExpression) {
            return evaluateUnaryExpression((IASTUnaryExpression) exp);
        }
        if (exp instanceof IASTConditionalExpression) {
            IASTConditionalExpression cexpr = (IASTConditionalExpression) exp;
            Long v = evaluate(cexpr.getLogicalConditionExpression());
            if (v == null)
                return v;
            if (v.longValue() == 0) {
                return evaluate(cexpr.getNegativeResultExpression());
            }
            final IASTExpression pe = cexpr.getPositiveResultExpression();
            if (pe == null) // gnu-extension allows to omit the positive expression.
                return v;
            return evaluate(pe);
        }
        if (exp instanceof IASTIdExpression) {
            IASTName name = ((IASTIdExpression) exp).getName();
            IBinding b = name.resolvePreBinding();
            Long value = env == null ? null : env.getValue(b);
            constValuedNames.put(name, value);
            return value;
        }
        if (exp instanceof IASTLiteralExpression) {
            IASTLiteralExpression litEx = (IASTLiteralExpression) exp;
            switch (litEx.getKind()) {
            case IASTLiteralExpression.lk_false:
            case IASTLiteralExpression.lk_nullptr:
                return Long.valueOf(0);
            case IASTLiteralExpression.lk_true:
                return Long.valueOf(1);
            case IASTLiteralExpression.lk_integer_constant:
                return getNumber(litEx.getValue());
            case IASTLiteralExpression.lk_char_constant:
                return null;
            }
        }
        if (exp instanceof IASTFunctionCallExpression || exp instanceof ICPPASTSimpleTypeConstructorExpression) {
            return null; // The value will be obtained from the evaluation.
        }
        return null;
    }

    // from org.eclipse.cdt.internal.core.parser.scanner.ExpressionEvaluator
    private static long getNumber(char[] image) {
        // Integer constants written in binary are a non-standard extension
        // supported by GCC since 4.3 and by some other C compilers
        // They consist of a prefix 0b or 0B, followed by a sequence of 0 and 1 digits
        // see http://gcc.gnu.org/onlinedocs/gcc/Binary-constants.html
        boolean isBin = false;

        boolean isHex = false;
        boolean isOctal = false;

        int pos = 0;
        if (image.length > 1) {
            if (image[0] == '0') {
                switch (image[++pos]) {
                case 'b':
                case 'B':
                    isBin = true;
                    ++pos;
                    break;
                case 'x':
                case 'X':
                    isHex = true;
                    ++pos;
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    isOctal = true;
                    ++pos;
                    break;
                }
            }
        }
        if (isBin) {
            return getNumber(image, 2, image.length, 2, IProblem.SCANNER_BAD_BINARY_FORMAT);
        }
        if (isHex) {
            return getNumber(image, 2, image.length, 16, IProblem.SCANNER_BAD_HEX_FORMAT);
        }
        if (isOctal) {
            return getNumber(image, 1, image.length, 8, IProblem.SCANNER_BAD_OCTAL_FORMAT);
        }
        return getNumber(image, 0, image.length, 10, IProblem.SCANNER_BAD_DECIMAL_FORMAT);
    }

    // from org.eclipse.cdt.internal.core.parser.scanner.ExpressionEvaluator
    private static Long getNumber(char[] tokenImage, int from, int to, int base, int problemID) {
        if (from == to) {
            return null;
        }
        long result = 0;
        int i = from;
        for (; i < to; i++) {
            int digit = getDigit(tokenImage[i]);
            if (digit >= base) {
                break;
            }
            result = result * base + digit;
        }
        for (; i < to; i++) {
            switch (tokenImage[i]) {
            case 'u':
            case 'l':
            case 'U':
            case 'L':
                break;
            default:
                return null;
            }
        }
        return result;
    }

    // from org.eclipse.cdt.internal.core.parser.scanner.ExpressionEvaluator
    private static int getDigit(char c) {
        switch (c) {
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return c - '0';
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
            return c - 'a' + 10;
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
            return c - 'A' + 10;
        }
        return Integer.MAX_VALUE;
    }

    private Long evaluateBinaryExpression(IASTBinaryExpression exp) {
        final int op = exp.getOperator();

        if (isAssignmentOperator(op)) {
            if (op == IASTBinaryExpression.op_assign) {
                IASTName lhsName = ASTUtil.getIdExpression(exp.getOperand1());
                Long rhsValue = evaluate(exp.getOperand2());
                if (lhsName != null && rhsValue != null) {
                    IBinding binding = lhsName.resolveBinding();
                    if (env == null)
                        env = ConstEnv.EMPTY;
                    env = env.set(binding, rhsValue);
                    constValuedNames.put(lhsName, rhsValue);
                    return rhsValue;
                }
            }
            env = ConstEnv.EMPTY;
            return evaluate(exp.getOperand2());
        }

        switch (op) {
        case IASTBinaryExpression.op_equals:
            if (exp.getOperand1().equals(exp.getOperand2()))
                return Long.valueOf(1);
            break;
        case IASTBinaryExpression.op_notequals:
            if (exp.getOperand1().equals(exp.getOperand2()))
                return Long.valueOf(0);
            break;
        }

        final Long o1 = evaluate(exp.getOperand1());
        final Long o2 = evaluate(exp.getOperand2());
        if (o1 == null || o2 == null)
            return null;

        return applyBinaryOperator(op, o1.longValue(), o2.longValue());
    }

    private boolean isAssignmentOperator(int op) {
        switch (op) {
        case IASTBinaryExpression.op_assign:
        case IASTBinaryExpression.op_binaryAndAssign:
        case IASTBinaryExpression.op_binaryOrAssign:
        case IASTBinaryExpression.op_binaryXorAssign:
        case IASTBinaryExpression.op_divideAssign:
        case IASTBinaryExpression.op_minusAssign:
        case IASTBinaryExpression.op_moduloAssign:
        case IASTBinaryExpression.op_multiplyAssign:
        case IASTBinaryExpression.op_plusAssign:
        case IASTBinaryExpression.op_shiftLeftAssign:
        case IASTBinaryExpression.op_shiftRightAssign:
            return true;
        default:
            return false;
        }
    }

    private Long applyBinaryOperator(final int op, final long v1, final long v2) {
        switch (op) {
        case IASTBinaryExpression.op_multiply:
            return v1 * v2;
        case IASTBinaryExpression.op_divide:
            if (v2 == 0)
                return null;
            return v1 / v2;
        case IASTBinaryExpression.op_modulo:
            if (v2 == 0)
                return null;
            return v1 % v2;
        case IASTBinaryExpression.op_plus:
            return v1 + v2;
        case IASTBinaryExpression.op_minus:
            return v1 - v2;
        case IASTBinaryExpression.op_shiftLeft:
            return v1 << v2;
        case IASTBinaryExpression.op_shiftRight:
            return v1 >> v2;
        case IASTBinaryExpression.op_lessThan:
            return v1 < v2 ? Long.valueOf(1) : Long.valueOf(0);
        case IASTBinaryExpression.op_greaterThan:
            return v1 > v2 ? Long.valueOf(1) : Long.valueOf(0);
        case IASTBinaryExpression.op_lessEqual:
            return v1 <= v2 ? Long.valueOf(1) : Long.valueOf(0);
        case IASTBinaryExpression.op_greaterEqual:
            return v1 >= v2 ? Long.valueOf(1) : Long.valueOf(0);
        case IASTBinaryExpression.op_binaryAnd:
            return v1 & v2;
        case IASTBinaryExpression.op_binaryXor:
            return v1 ^ v2;
        case IASTBinaryExpression.op_binaryOr:
            return v1 | v2;
        case IASTBinaryExpression.op_logicalAnd:
            return v1 != 0 && v2 != 0 ? Long.valueOf(1) : Long.valueOf(0);
        case IASTBinaryExpression.op_logicalOr:
            return v1 != 0 || v2 != 0 ? Long.valueOf(1) : Long.valueOf(0);
        case IASTBinaryExpression.op_equals:
            return v1 == v2 ? Long.valueOf(1) : Long.valueOf(0);
        case IASTBinaryExpression.op_notequals:
            return v1 != v2 ? Long.valueOf(1) : Long.valueOf(0);
        case IASTBinaryExpression.op_max:
            return Math.max(v1, v2);
        case IASTBinaryExpression.op_min:
            return Math.min(v1, v2);
        }
        return null;
    }

    private Long evaluateUnaryExpression(IASTUnaryExpression exp) {
        final int unaryOp = exp.getOperator();

        if (unaryOp == IASTUnaryExpression.op_sizeof || unaryOp == IASTUnaryExpression.op_amper
                || unaryOp == IASTUnaryExpression.op_star || unaryOp == IASTUnaryExpression.op_sizeofParameterPack) {
            return null;
        }

        Long value = evaluate(exp.getOperand());
        if (value == null)
            return value;

        switch (unaryOp) {
        case IASTUnaryExpression.op_prefixIncr:
        case IASTUnaryExpression.op_postFixIncr:
            setEnvValue(exp.getOperand(), value.longValue() + 1);
        case IASTUnaryExpression.op_prefixDecr:
        case IASTUnaryExpression.op_postFixDecr:
            setEnvValue(exp.getOperand(), value.longValue() - 1);
        }

        return applyUnaryOperator(unaryOp, value.longValue());
    }

    private void setEnvValue(IASTExpression exp, long value) {
        IASTName lhsName = ASTUtil.getIdExpression(exp);
        if (lhsName != null) {
            if (env == null)
                env = ConstEnv.EMPTY;
            env = env.set(lhsName.resolveBinding(), value);
            constValuedNames.put(lhsName, value);
        } else {
            env = ConstEnv.EMPTY;
        }
    }

    private Long applyUnaryOperator(final int unaryOp, final long value) {
        switch (unaryOp) {
        case IASTUnaryExpression.op_bracketedPrimary:
        case IASTUnaryExpression.op_plus:
            return value;
        }

        switch (unaryOp) {
        case IASTUnaryExpression.op_prefixIncr:
            return value + 1;
        case IASTUnaryExpression.op_postFixIncr:
            return value;
        case IASTUnaryExpression.op_prefixDecr:
            return value - 1;
        case IASTUnaryExpression.op_postFixDecr:
            return value;
        case IASTUnaryExpression.op_minus:
            return -value;
        case IASTUnaryExpression.op_tilde:
            return ~value;
        case IASTUnaryExpression.op_not:
            return value == 0 ? Long.valueOf(1) : Long.valueOf(0);
        }
        return null;
    }
}
