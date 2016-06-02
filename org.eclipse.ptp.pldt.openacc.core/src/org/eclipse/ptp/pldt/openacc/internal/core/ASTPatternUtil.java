package org.eclipse.ptp.pldt.openacc.internal.core;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.LinearExpression;

public class ASTPatternUtil {

	private ASTPatternUtil() {
	}
	
	public static IASTFunctionCallExpression getFuncExpression(IASTExpression expr) {
		if (!(expr instanceof IASTFunctionCallExpression))
			return null;
		return (IASTFunctionCallExpression) expr;
	}

	public static Pair<IASTExpression, IASTExpression> getAssignEq(IASTExpression expr) {
		if (!(expr instanceof IASTBinaryExpression))
			return null;

		IASTBinaryExpression binExp = (IASTBinaryExpression) expr;
		switch (binExp.getOperator()) {
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
			return new Pair<IASTExpression, IASTExpression>(binExp.getOperand1(), binExp.getOperand2());
		}

		return null;
	}

	public static Pair<IASTExpression, IASTExpression> getAssignment(IASTExpression expr) {
		if (!(expr instanceof IASTBinaryExpression))
			return null;

		IASTBinaryExpression binExp = (IASTBinaryExpression) expr;
		if (binExp.getOperator() != IASTBinaryExpression.op_assign)
			return null;

		return new Pair<IASTExpression, IASTExpression>(binExp.getOperand1(), binExp.getOperand2());
	}

	public static Integer getConstantExpression(IASTExpression expr) {
		if (!(expr instanceof IASTLiteralExpression))
			return null;

		IASTLiteralExpression literal = (IASTLiteralExpression) expr;
		if (literal.getKind() != IASTLiteralExpression.lk_integer_constant)
			return null;

		try {
			return Integer.parseInt(String.valueOf(literal.getValue()));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static IASTExpression getIncrDecr(IASTExpression expr) {
		if (!(expr instanceof IASTUnaryExpression))
			return null;

		IASTUnaryExpression unaryExp = (IASTUnaryExpression) expr;
		switch (unaryExp.getOperator()) {
		case IASTUnaryExpression.op_prefixIncr:
		case IASTUnaryExpression.op_postFixIncr:
		case IASTUnaryExpression.op_prefixDecr:
		case IASTUnaryExpression.op_postFixDecr:
			return unaryExp.getOperand();
		}
		return null;
	}

	public static IASTName getIdExpression(IASTExpression expr) {
		if (!(expr instanceof IASTIdExpression))
			return null;

		return ((IASTIdExpression) expr).getName();
	}

	public static Pair<IASTName, LinearExpression[]> getMultidimArrayAccess(IASTExpression expr) {
		if (!(expr instanceof IASTArraySubscriptExpression))
			return null;

		IASTArraySubscriptExpression arrSub = (IASTArraySubscriptExpression) expr;
		IASTExpression array = arrSub.getArrayExpression();
		IASTInitializerClause subscript = arrSub.getArgument();

		IASTName name;
		LinearExpression[] prevSubscripts;
		if (array instanceof IASTArraySubscriptExpression) {
			Pair<IASTName, LinearExpression[]> nested = getMultidimArrayAccess(array);
			if (nested == null)
				return null;
			name = nested.getFirst();
			prevSubscripts = nested.getSecond();
		} else {
			name = getIdExpression(array);
			if (name == null) {
				Pair<IASTName, IASTName> fieldRef = ASTPatternUtil.getSimpleFieldReference(array);
				name = fieldRef == null ? null : fieldRef.getSecond();
			}
			prevSubscripts = new LinearExpression[0];
		}

		if (name == null || !(subscript instanceof IASTExpression))
			return null;

		LinearExpression thisSubscript = LinearExpression.createFrom((IASTExpression) subscript);
		return new Pair<IASTName, LinearExpression[]>(name, concat(prevSubscripts, thisSubscript));
	}

	static LinearExpression[] concat(LinearExpression[] prevSubscripts, LinearExpression thisSubscript) {
		// If any of the subscript expressions is not linear, treat the array access like a scalar access
		// (i.e., ignore all subscripts)
		if (prevSubscripts == null || thisSubscript == null)
			return null;

		LinearExpression[] result = new LinearExpression[prevSubscripts.length + 1];
		System.arraycopy(prevSubscripts, 0, result, 0, prevSubscripts.length);
		result[result.length - 1] = thisSubscript;
		return result;
	}

	public static Pair<IASTName, IASTName> getSimpleFieldReference(IASTExpression expr) {
		if (!(expr instanceof IASTFieldReference))
			return null;

		IASTFieldReference fieldReference = (IASTFieldReference) expr;
		IASTName owner = getIdExpression(fieldReference.getFieldOwner());
		IASTName field = fieldReference.getFieldName();

		if (owner == null || field == null || fieldReference.isPointerDereference())
			return null;
		else
			return new Pair<IASTName, IASTName>(owner, field);
	}

	public static boolean isDefinition(IASTName name) {
		IASTStatement defStmt = ASTUtil.findNearestAncestor(name, IASTStatement.class);
		IASTUnaryExpression defUnaryExpr = ASTUtil.findNearestAncestor(name, IASTUnaryExpression.class);
		IASTBinaryExpression defBinaryExpr = ASTUtil.findNearestAncestor(name, IASTBinaryExpression.class);
		if (defStmt instanceof IASTDeclarationStatement) {
			if (((IASTDeclarationStatement) defStmt).getDeclaration() instanceof IASTSimpleDeclaration) {
				IASTSimpleDeclaration simpleDec = (IASTSimpleDeclaration) (((IASTDeclarationStatement) defStmt)
						.getDeclaration());
				// int x; int x, y; int x = 1;
				for (IASTDeclarator decl : simpleDec.getDeclarators()) {
					if (decl.getName().equals(name)) {
						return true;
					}
				}
			}
		} else {
			if ((defStmt instanceof IASTExpressionStatement
					&& ((IASTExpressionStatement) defStmt).getExpression() instanceof IASTUnaryExpression)
					|| defUnaryExpr != null) {
				// x++; x--; ++x; --x;
				IASTUnaryExpression unary;
				if ((defStmt instanceof IASTExpressionStatement
						&& ((IASTExpressionStatement) defStmt).getExpression() instanceof IASTUnaryExpression)) {
					unary = (IASTUnaryExpression) (((IASTExpressionStatement) defStmt).getExpression());
				} else {
					unary = defUnaryExpr;
				}
				if (unary.getOperator() == IASTUnaryExpression.op_postFixIncr
						|| unary.getOperator() == IASTUnaryExpression.op_prefixIncr
						|| unary.getOperator() == IASTUnaryExpression.op_postFixDecr
						|| unary.getOperator() == IASTUnaryExpression.op_prefixIncr) {
					if (unary.getOperand() instanceof IASTIdExpression) {
						if (((IASTIdExpression) unary.getOperand()).getName().equals(name)) {
							return true;
						}
					} else if (unary.getOperand() instanceof IASTArraySubscriptExpression) {
						IASTArraySubscriptExpression arrSubExpr = (IASTArraySubscriptExpression) unary.getOperand();
						if (arrSubExpr.getArrayExpression() instanceof IASTIdExpression) {
							if (((IASTIdExpression) arrSubExpr.getArrayExpression()).getName().equals(name)) {
								return true;
							}
						}
					}
				}
			}
	
			if ((defStmt instanceof IASTExpressionStatement
					&& ((IASTExpressionStatement) defStmt).getExpression() instanceof IASTBinaryExpression)
					|| defBinaryExpr != null) {
				// x = 1; x += 1; ...
				IASTBinaryExpression binary;
				if ((defStmt instanceof IASTExpressionStatement
						&& ((IASTExpressionStatement) defStmt).getExpression() instanceof IASTUnaryExpression)) {
					binary = (IASTBinaryExpression) (((IASTExpressionStatement) defStmt).getExpression());
				} else {
					binary = defBinaryExpr;
				}
				if (binary.getOperator() == IASTBinaryExpression.op_assign
						|| binary.getOperator() == IASTBinaryExpression.op_binaryAndAssign
						|| binary.getOperator() == IASTBinaryExpression.op_binaryOrAssign
						|| binary.getOperator() == IASTBinaryExpression.op_binaryXorAssign
						|| binary.getOperator() == IASTBinaryExpression.op_divideAssign
						|| binary.getOperator() == IASTBinaryExpression.op_minusAssign
						|| binary.getOperator() == IASTBinaryExpression.op_moduloAssign
						|| binary.getOperator() == IASTBinaryExpression.op_multiplyAssign
						|| binary.getOperator() == IASTBinaryExpression.op_plusAssign
						|| binary.getOperator() == IASTBinaryExpression.op_shiftLeftAssign
						|| binary.getOperator() == IASTBinaryExpression.op_shiftRightAssign) {
					if (binary.getOperand1() instanceof IASTIdExpression) {
						if (((IASTIdExpression) binary.getOperand1()).getName().equals(name)) {
							return true;
						}
					} else if (binary.getOperand1() instanceof IASTArraySubscriptExpression) {
						IASTArraySubscriptExpression arrSubExpr = (IASTArraySubscriptExpression) binary.getOperand1();
						if (arrSubExpr.getArrayExpression() instanceof IASTIdExpression) {
							if (((IASTIdExpression) arrSubExpr.getArrayExpression()).getName().equals(name)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

}
