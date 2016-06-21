/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.dependence;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTPatternUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.BindingComparator;

/**
 * An decomposition of a {@link IASTExpression} as a linear expression.
 * <p>
 * A linear expression has the form
 * <i>c</i><sub>0</sub> + <i>c</i><sub>1</sub><i>x</i><sub>1</sub> + <i>c</i><sub>2</sub><i>x</i><sub>2</sub> + ... + <i>c</i>
 * <sub><i>n</i></sub><i>x</i><sub><i>n</i></sub>,
 * where the <i>c</i>'s are constants (the <i>coefficients</i>) and the <i>x</i>'s are variables. The constant <i>c</i>
 * <sub>0</sub> is called the <i>constant coefficient</i>.
 * 
 * @author Jeff Overbey
 */
public class LinearExpression {
	/**
	 * Factory method. Determines if the given expression is a linear expression, returning a
	 * {@link LinearExpression} if it is and <code>null</code> if it is not.
	 * 
	 * @param expr
	 *            expression to analyze
	 * @return {@link LinearExpression} or <code>null</code>
	 */
	public static LinearExpression createFrom(IASTExpression expr) {
		try {
			return translateExpression(expr);
		} catch (UnsupportedOperationException e) {
			return null;
		}
	}

	private static LinearExpression translateExpression(IASTExpression expr) throws UnsupportedOperationException {
		if (expr instanceof IASTBinaryExpression) {
			return translate((IASTBinaryExpression) expr);
		} else if (expr instanceof IASTUnaryExpression) {
			return translate((IASTUnaryExpression) expr);
		} else if (expr instanceof IASTLiteralExpression) {
			return translate((IASTLiteralExpression) expr);
		} else if (expr instanceof IASTIdExpression) {
			return translate((IASTIdExpression) expr);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private static LinearExpression translate(IASTBinaryExpression expr) throws UnsupportedOperationException {
		switch (expr.getOperator()) {
		case IASTBinaryExpression.op_plus: {
			LinearExpression lhs = translateExpression(expr.getOperand1());
			LinearExpression rhs = translateExpression(expr.getOperand2());
			return lhs.plus(rhs);
		}

		case IASTBinaryExpression.op_minus: {
			LinearExpression lhs = translateExpression(expr.getOperand1());
			LinearExpression rhs = translateExpression(expr.getOperand2());
			return lhs.plus(rhs.times(-1));
		}

		case IASTBinaryExpression.op_multiply: {
			LinearExpression lhs = translateExpression(expr.getOperand1());
			LinearExpression rhs = translateExpression(expr.getOperand2());
			return lhs.times(rhs);
		}

		default:
			throw new UnsupportedOperationException();
		}
	}

	private static LinearExpression translate(IASTUnaryExpression expr) throws UnsupportedOperationException {
		switch (expr.getOperator()) {
		case IASTUnaryExpression.op_bracketedPrimary:
		case IASTUnaryExpression.op_plus:
			return translateExpression(expr.getOperand());

		case IASTUnaryExpression.op_minus:
			return translateExpression(expr.getOperand()).times(-1);

		default:
			throw new UnsupportedOperationException();
		}
	}

	private static LinearExpression translate(IASTLiteralExpression expr) throws UnsupportedOperationException {
		Integer value = ASTPatternUtil.getConstantExpression(expr);
		if (value != null)
			return new LinearExpression(value.intValue());
		else
			throw new UnsupportedOperationException();
	}

	private static LinearExpression translate(IASTIdExpression expr) {
		return new LinearExpression(expr.getName().resolveBinding());
	}

	private final Map<IBinding, Integer> coefficients;
	private final int constantCoefficient;

	private LinearExpression(int constant) {
		this.coefficients = Collections.emptyMap();
		this.constantCoefficient = constant;
	}

	private LinearExpression(IBinding variable) {
		this.coefficients = Collections.singletonMap(variable, 1);
		this.constantCoefficient = 0;
	}

	private LinearExpression(Map<IBinding, Integer> coefficients, int constantCoefficient) {
		this.coefficients = Collections.unmodifiableMap(coefficients);
		this.constantCoefficient = constantCoefficient;
	}

	/**
	 * @return the set of all variables occurring in this expression
	 */
	public Set<IBinding> getVariables() {
		return Collections.unmodifiableSet(coefficients.keySet());
	}

	/**
	 * @return the coefficient of the given variable in this expression (0 if the variable does not occur in this expression)
	 */
	public int getCoefficient(IBinding variable) {
		if (!coefficients.containsKey(variable))
			return 0;
		return coefficients.get(variable);
	}

	/**
	 * @return the constant coefficient for this linear expression
	 */
	public int getConstantCoefficient() {
		return constantCoefficient;
	}

	private LinearExpression plus(LinearExpression rhs) {
		Map<IBinding, Integer> result = new HashMap<IBinding, Integer>(coefficients);
		for (IBinding var : rhs.coefficients.keySet()) {
			if (!result.containsKey(var)) {
				result.put(var, 0);
			}
			result.put(var, result.get(var) + rhs.coefficients.get(var));
		}
		return new LinearExpression(result, this.constantCoefficient + rhs.constantCoefficient);
	}

	private LinearExpression times(LinearExpression rhs) throws UnsupportedOperationException {
		if (this.isConstant())
			return rhs.times(this.constantCoefficient);
		else if (rhs.isConstant())
			return this.times(rhs.constantCoefficient);
		else
			throw new UnsupportedOperationException();
	}

	private LinearExpression times(int multiplier) {
		Map<IBinding, Integer> result = new HashMap<IBinding, Integer>(coefficients.size());
		for (IBinding var : coefficients.keySet()) {
			result.put(var, coefficients.get(var) * multiplier);
		}
		return new LinearExpression(result, this.constantCoefficient * multiplier);
	}

	private boolean isConstant() {
		for (IBinding var : coefficients.keySet()) {
			if (coefficients.get(var) != 0) {
				return false;
			}
		}
		return true;
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
