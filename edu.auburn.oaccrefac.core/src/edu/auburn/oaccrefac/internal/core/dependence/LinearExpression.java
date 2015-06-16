package edu.auburn.oaccrefac.internal.core.dependence;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.BindingComparator;

public class LinearExpression {
    private static interface ILENode {
        ILENode negated();

        ILENode add(ILENode rhs);

        ILENode multiply(ILENode negated) throws DependenceTestFailure;
    }

    private static interface ILEPrimary extends ILENode {
        ILEPrimary negated();

        ILEPrimary multiply(ILENode negated) throws DependenceTestFailure;
    }

    private static final class ILEConst implements ILEPrimary {
        private final int value;

        public ILEConst(int value) {
            this.value = value;
        }

        @Override
        public ILEPrimary negated() {
            return new ILEConst(-value);
        }

        @Override
        public ILENode add(ILENode that) {
            if (that instanceof ILEConst) {
                return new ILEConst(value + ((ILEConst) that).value);
            } else if (that instanceof ILESum) {
                return ((ILESum) that).append(this);
            } else {
                return new ILESum(this, (ILEPrimary) that);
            }
        }

        @Override
        public ILEPrimary multiply(ILENode that) throws DependenceTestFailure {
            if (that instanceof ILEConst) {
                return new ILEConst(value * ((ILEConst) that).value);
            } else if (that instanceof ILEScaledVar) {
                ILEScaledVar sv = (ILEScaledVar) that;
                return new ILEScaledVar(value * sv.scale, sv.variable);
            } else {
                throw new DependenceTestFailure("Unsupported expression");
            }
        }
    }

    private static final class ILEScaledVar implements ILEPrimary {
        private final int scale;
        private final IBinding variable;

        public ILEScaledVar(int scale, IBinding variable) {
            this.scale = scale;
            this.variable = variable;
        }

        @Override
        public ILEPrimary negated() {
            return new ILEScaledVar(-scale, variable);
        }

        @Override
        public ILENode add(ILENode that) {
            if (that instanceof ILESum) {
                return ((ILESum) that).append(this);
            } else {
                return new ILESum(this, (ILEPrimary) that);
            }
        }

        @Override
        public ILEPrimary multiply(ILENode that) throws DependenceTestFailure {
            if (that instanceof ILEConst) {
                return new ILEScaledVar(scale * ((ILEConst) that).value, variable);
            } else {
                throw new DependenceTestFailure("Unsupported expression");
            }
        }
    }

    private static final class ILESum implements ILENode {
        private final ILEPrimary[] addends;

        public ILESum(ILEPrimary... addends) {
            this.addends = addends;
        }

        @Override
        public ILESum negated() {
            ILEPrimary[] newAddends = new ILEPrimary[addends.length];
            for (int i = 0; i < addends.length; i++) {
                newAddends[i] = addends[i].negated();
            }
            return new ILESum(newAddends);
        }

        @Override
        public ILENode add(ILENode that) {
            if (that instanceof ILESum) {
                ILESum sum2 = (ILESum) that;
                ILEPrimary[] newAddends = new ILEPrimary[addends.length + sum2.addends.length];
                System.arraycopy(addends, 0, newAddends, 0, addends.length);
                System.arraycopy(sum2.addends, 0, newAddends, addends.length, sum2.addends.length);
                return new ILESum(newAddends);
            } else {
                return this.append((ILEPrimary) that);
            }
        }

        private ILENode append(ILEPrimary that) {
            ILEPrimary[] newAddends = new ILEPrimary[addends.length + 1];
            System.arraycopy(addends, 0, newAddends, 0, addends.length);
            newAddends[addends.length] = that;
            return new ILESum(newAddends);
        }

        @Override
        public ILENode multiply(ILENode that) throws DependenceTestFailure {
            throw new DependenceTestFailure("Unsupported expression");
        }
    }

    public static LinearExpression createFrom(IASTExpression expr) {
        try {
            return new LinearExpression(expr);
        } catch (DependenceTestFailure e) {
            return null;
        }
    }

    private static void addToCoefficient(IBinding variable, int valueToAdd, Map<IBinding, Integer> coefficients) {
        int curValue = coefficients.containsKey(variable) ? coefficients.get(variable) : 0;
        coefficients.put(variable, curValue + valueToAdd);
    }

    private static ILESum getSum(ILENode expr) {
        if (expr instanceof ILESum) {
            return (ILESum) expr;
        } else {
            return new ILESum((ILEPrimary) expr);
        }
    }

    private static ILENode translateExpression(IASTExpression expr) throws DependenceTestFailure {
        if (expr instanceof IASTBinaryExpression) {
            return translate((IASTBinaryExpression) expr);
        } else if (expr instanceof IASTUnaryExpression) {
            return translate((IASTUnaryExpression) expr);
        } else if (expr instanceof IASTLiteralExpression) {
            return translate((IASTLiteralExpression) expr);
        } else if (expr instanceof IASTIdExpression) {
            return translate((IASTIdExpression) expr);
        } else {
            throw unsupported(expr);
        }
    }

    private static ILENode translate(IASTBinaryExpression expr) throws DependenceTestFailure {
        switch (expr.getOperator()) {
        case IASTBinaryExpression.op_plus: {
            ILENode lhs = translateExpression(expr.getOperand1());
            ILENode rhs = translateExpression(expr.getOperand2());
            return lhs.add(rhs);
        }

        case IASTBinaryExpression.op_minus: {
            ILENode lhs = translateExpression(expr.getOperand1());
            ILENode rhs = translateExpression(expr.getOperand2());
            return lhs.add(rhs.negated());
        }

        case IASTBinaryExpression.op_multiply: {
            ILENode lhs = translateExpression(expr.getOperand1());
            ILENode rhs = translateExpression(expr.getOperand2());
            return lhs.multiply(rhs);
        }

        default:
            throw unsupported(expr);
        }
    }

    private static ILENode translate(IASTUnaryExpression expr) throws DependenceTestFailure {
        switch (expr.getOperator()) {
        case IASTUnaryExpression.op_bracketedPrimary:
        case IASTUnaryExpression.op_plus:
            return translateExpression(expr.getOperand());

        case IASTUnaryExpression.op_minus:
            return translateExpression(expr.getOperand()).negated();

        default:
            throw unsupported(expr);
        }
    }

    private static ILENode translate(IASTLiteralExpression expr) throws DependenceTestFailure {
        Integer value = ASTUtil.getConstantExpression(expr);
        if (value != null)
            return new ILEConst(value.intValue());
        else
            throw unsupported(expr);
    }

    private static ILENode translate(IASTIdExpression expr) {
        return new ILEScaledVar(1, expr.getName().resolveBinding());
    }

    private static DependenceTestFailure unsupported(IASTNode node) {
        return new DependenceTestFailure(
                String.format("Unsupported construct on line %d (%s)", node.getFileLocation().getStartingLineNumber(), //
                        node.getClass().getSimpleName()));
    }

    // private final IBinding[] vars;
    // private final int[] coeffs;

    private final Map<IBinding, Integer> coefficients;
    private final int constantCoefficient;

    private LinearExpression(IASTExpression expr) throws DependenceTestFailure {
        ILESum sum = getSum(translateExpression(expr));

        Map<IBinding, Integer> coefficients = new HashMap<IBinding, Integer>();
        int constantCoefficient = 0;
        for (ILEPrimary p : sum.addends) {
            if (p instanceof ILEConst) {
                constantCoefficient += ((ILEConst) p).value;
            } else if (p instanceof ILEScaledVar) {
                addToCoefficient(((ILEScaledVar) p).variable, ((ILEScaledVar) p).scale, coefficients);
            } else {
                throw new IllegalStateException();
            }
        }

        this.coefficients = Collections.unmodifiableMap(coefficients);
        this.constantCoefficient = constantCoefficient;
    }

    public Map<IBinding, Integer> getCoefficients() {
        return coefficients;
    }

    public int getConstantCoefficient() {
        return constantCoefficient;
    }

    @Override
    public String toString() {
        IBinding[] vars = coefficients.keySet().toArray(new IBinding[coefficients.size()]);
        Arrays.sort(vars, new BindingComparator());

        int[] coeffs = new int[coefficients.size() + 1];
        coeffs[0] = constantCoefficient;
        for (int i = 0; i < vars.length; i++) {
            coeffs[i + 1] = coefficients.get(vars[i]);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < coeffs.length; i++) {
            if (i > 0)
                sb.append(" + ");
            sb.append(coeffs[i]);
            if (i > 0) {
                sb.append('*');
                sb.append(vars[i - 1].getName());
            }
        }
        return sb.toString();
    }
}
