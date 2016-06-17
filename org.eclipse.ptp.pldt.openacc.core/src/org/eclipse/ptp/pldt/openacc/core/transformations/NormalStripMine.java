package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class NormalStripMine extends ForLoopAlteration<StripMineCheck> {

	private int stripFactor;
	private boolean handleOverflow;
	private String newName;	

	public NormalStripMine(IASTRewrite rewriter, int stripFactor, boolean handleOverflow, String newName, StripMineCheck check) {
		super(rewriter, check);
		this.stripFactor = stripFactor;
		this.handleOverflow = handleOverflow;
		this.newName = newName;
	}
	
	@Override
	protected void doChange() {
		IASTForStatement loop = getLoop();
		ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(loop);
		String indexVar = inq.getIndexVariable().toString();
		if (newName.equals("")) {
			try {
				newName = createNewName(indexVar, loop.getScope().getParent());
			} catch (DOMException e) {
				e.printStackTrace();
				return;
			}
		}

		String innerInit, innerCond, innerIter, innerBody, inner;
		String outerInit, outerCond, outerIter, outer;
		IASTBinaryExpression condExpr = (IASTBinaryExpression) loop.getConditionExpression();
		String compOp = getOperatorAsString(condExpr);
		String ub = condExpr.getOperand2().getRawSignature();
		if (loop.getInitializerStatement() instanceof IASTDeclarationStatement) {
			innerInit = String.format("int %s = %s", indexVar, newName);
		} else {
			innerInit = String.format("%s = %s", indexVar, newName);
		}
		innerCond = getInnerCond(indexVar, newName, stripFactor, compOp, ub);
		innerIter = getInnerIter(loop, indexVar, ub, stripFactor);
		innerBody = loop.getBody().getRawSignature();
		if (!(loop.getBody() instanceof IASTCompoundStatement)) {
			IASTComment[] outerComments = getBodyComments(loop);
			for (IASTComment comment : outerComments) {
				innerBody = comment.getRawSignature() + "\n" + innerBody;
			}
			innerBody = compound(innerBody);
		}
		inner = forLoop(innerInit, innerCond, innerIter, innerBody);

		String initRhs;
		String initType;
		// TODO we're making a lot of typecast assumptions - be sure they won't break anything
		if (loop.getInitializerStatement() instanceof IASTExpressionStatement) {
			IASTExpressionStatement es = (IASTExpressionStatement) loop.getInitializerStatement();
			IASTBinaryExpression e = (IASTBinaryExpression) es.getExpression();
			initRhs = e.getOperand2().getRawSignature();
			IType type = e.getOperand1().getExpressionType();
			if (type instanceof ITypedef) {
				initType = ((ITypedef) type).getType().toString();
			} else {
				initType = type.toString();
			}
		} else {
			IASTDeclarationStatement ds = (IASTDeclarationStatement) loop.getInitializerStatement();
			IASTSimpleDeclaration dec = (IASTSimpleDeclaration) ds.getDeclaration();
			IASTEqualsInitializer init = (IASTEqualsInitializer) dec.getDeclarators()[0].getInitializer();
			initRhs = init.getInitializerClause().getRawSignature();
			initType = dec.getDeclSpecifier().getRawSignature();
		}
		outerInit = String.format("%s %s = %s", initType, newName, initRhs);
		outerCond = getOuterCond(newName, compOp, ub, stripFactor);
		outerIter = getOuterIter(newName, stripFactor);
		outer = forLoop(outerInit, outerCond, outerIter, compound(inner));
		this.replace(loop, outer);
		finalizeChanges();
	}

	protected String getOperatorAsString(IASTBinaryExpression condExpr) {
		String compOp;
		switch (condExpr.getOperator()) {
		case IASTBinaryExpression.op_lessEqual:
			compOp = "<=";
			break;
		case IASTBinaryExpression.op_lessThan:
			compOp = "<";
			break;
		default:
			throw new IllegalStateException();
		}
		return compOp;
	}

	protected String getOuterCond(String newName, String compOp, String ub, int numValue) {
		return String.format("%s %s %s", newName, compOp, ub);
	}

	protected String getOuterIter(String newName, int numFactor) {
		return String.format("%s += %d", newName, numFactor);
	}

	protected String getInnerCond(String indexVar, String newName, int numFactor,
			String compOp, String ub) {
		return parenth(String.format("%s <  %s + %d && %s %s %s", indexVar, newName, numFactor, indexVar, compOp, ub));
	}

	protected String getInnerIter(IASTForStatement loop, String indexVar, String ub, int numValue) {
		return loop.getIterationExpression().getRawSignature();
	}
	
}
