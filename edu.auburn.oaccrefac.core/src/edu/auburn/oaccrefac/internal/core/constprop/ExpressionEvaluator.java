/*******************************************************************************
 * Copyright (c) 2004, 2008, 2014, 2015 IBM Corporation, Wind River Systems, Inc. and others.
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

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.parser.IProblem;
import org.eclipse.core.runtime.NullProgressMonitor;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.core.dependence.AddressTakenAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Evaluates a C expression in a given environment.
 * <p>
 * Since C expressions can have side effects (e.g., <code>a + (++b)</code>), the
 * {@link #evaluate(IASTExpression, ConstEnv)} method returns both the value of the expression and the new constant
 * environment that resulting from its evaluation.
 * <p>
 * Used by {@link ConstantPropagation}.
 * <p>
 * Primarily based on org.eclipse.cdt.internal.core.dom.parser.Value with portions (getNumber) based on
 * org.eclipse.cdt.internal.core.parser.scanner.ExpressionEvaluator
 */
public class ExpressionEvaluator {

    /** Result returned by {@link ExpressionEvaluator#evaluate(IASTExpression, ConstEnv)}. */
    public static final class Result {
        /** The value of the expression being evaluated. */
        public final Long value;

        /** The constant environment that results after the node has been evaluated. */
        public final ConstEnv environment;

        /** Maps constant-valued {@link IASTName} nodes under the node being evaluated to their constant values. */
        public final Map<IASTName, Long> constValuedNames;

        private Result(Long value, ConstEnv environment, Map<IASTName, Long> constValuedNames) {
            this.value = value;
            this.environment = environment;
            this.constValuedNames = constValuedNames;
        }
    }

    /**
     * Evaluates the effects of an expression on a constant environment.
     * <p>
     * For example, evaluating the expression <code>b = 2+(a++);</code> in the constant environment [a=1] results in the
     * constant environment [a=2,b=3].
     * 
     * @see Result
     */
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

    protected ExpressionEvaluator() {
        this(ConstEnv.EMPTY);
    }

    /**
     * Computes the canonical representation of the value of the expression. Returns a {@code Number} for numerical
     * values or {@code null}, otherwise.
     * 
     * @throws UnknownValueException
     */
    public final Long evaluate(IASTExpression exp) {
        if (exp == null)
            return null;

        setAllNamesToNull(exp);

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
                return null;
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
            Long value = evaluateName(name);
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
            case IASTLiteralExpression.lk_float_constant:
                return null;
            }
        }
        if (exp instanceof IASTFunctionCallExpression) {
            IASTFunctionDefinition function = ASTUtil.findNearestAncestor(exp, IASTFunctionDefinition.class);
            // Conservatively assume that a function call might change every variable
            // whose address is taken (via pointers)...
            setAllAliasedVarsToNull(function);
            // ...but otherwise it will not change any local variables
            return null;
        }

        env = ConstEnv.EMPTY;
        return null;
    }

    private void setAllNamesToNull(IASTExpression exp) {
        exp.accept(new ASTVisitor(true) {
            @Override
            public int visit(IASTName name) {
                constValuedNames.put(name, null);
                return PROCESS_CONTINUE;
            }
        });
    }

    private void setAllAliasedVarsToNull(IASTFunctionDefinition function) {
        AddressTakenAnalysis addressTakenAnalysis = AddressTakenAnalysis.forFunction(function, new NullProgressMonitor());
        for (IVariable var : addressTakenAnalysis.getAddressTakenVariables()) {
            env = env.without(var);
        }
    }

    protected Long evaluateName(IASTName name) {
        IBinding b = name.resolvePreBinding();
        return env == null ? null : env.getValue(b);
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
                if (rhsValue != null) {
                    if (lhsName != null) {
                        IBinding binding = lhsName.resolveBinding();
                        if (env == null)
                            env = ConstEnv.EMPTY;
                        if (ConstantPropagation.canTrackConstantValues(binding)
                                && ConstantPropagation.isInTrackedRange(binding, rhsValue)) {
                            env = env.set(binding, rhsValue);
                            constValuedNames.put(lhsName, rhsValue);
                        }
                        return rhsValue;
                    } else if (ASTUtil.getMultidimArrayAccess(exp.getOperand1()) != null) {
                        return rhsValue;
                    }
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
            if (multiplyWillOverflow(v1, v2))
                return null;
            else
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
            if (addWillOverflow(v1, v2))
                return null;
            else
                return v1 + v2;
        case IASTBinaryExpression.op_minus:
            if (subtractWillOverflow(v1, v2))
                return null;
            else
                return v1 - v2;
        case IASTBinaryExpression.op_shiftLeft:
            return v1 << v2;
        case IASTBinaryExpression.op_shiftRight:
            return null; // Whether signed or unsigned is implementation-dependent
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

    // See discussion at
    // http://stackoverflow.com/questions/1657834/how-can-i-check-if-multiplying-two-numbers-in-java-will-cause-an-overflow
    // for various alternatives. The check for negating Long.MIN_VALUE is mine.
    public static boolean multiplyWillOverflow(long v1, long v2) {
        long maximum = Long.signum(v1) == Long.signum(v2) ? Long.MAX_VALUE : Long.MIN_VALUE;

        if (v1 == 0 || v2 == 0)
            return false;

        if (v2 == -1 && maximum < 0)
            return v1 == Long.MIN_VALUE;
        else if (v1 == -1 && maximum < 0)
            return v2 == Long.MIN_VALUE;

        return v2 > 0 && v2 > maximum / v1 || v2 < 0 && v2 < maximum / v1;
    }

    // See discussion at http://codereview.stackexchange.com/questions/6255/int-overflow-check-in-java
    public static boolean addWillOverflow(long v1, long v2) {
        if (v1 >= 0)
            return Long.MAX_VALUE - v1 < v2;
        else
            return Long.MIN_VALUE - v1 > v2;
    }

    public static boolean subtractWillOverflow(long v1, long v2) {
        if (v2 >= 0)
            return Long.MIN_VALUE + v2 > v1;
        else
            return Long.MAX_VALUE + v2 < v1;
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
            IBinding binding = lhsName.resolveBinding();
            if (ConstantPropagation.canTrackConstantValues(binding)
                    && ConstantPropagation.isInTrackedRange(binding, value)) {
                env = env.set(binding, value);
                constValuedNames.put(lhsName, value);
            }
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
