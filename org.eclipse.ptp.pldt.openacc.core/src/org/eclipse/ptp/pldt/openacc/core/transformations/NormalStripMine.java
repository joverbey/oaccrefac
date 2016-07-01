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
		if (newName.equals("")) { //$NON-NLS-1$
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
			IASTSimpleDeclaration s = (IASTSimpleDeclaration) ((IASTDeclarationStatement) loop.getInitializerStatement()).getDeclaration();
			innerInit = String.format("%s %s = %s", s.getDeclSpecifier().getRawSignature(), indexVar, newName); //$NON-NLS-1$
		} else {
			innerInit = String.format("%s = %s", indexVar, newName); //$NON-NLS-1$
		}
		if(handleOverflow) {
			innerCond = String.format("%s < %s + %d && %s %s %s", indexVar, newName, stripFactor, indexVar, compOp, ub); //$NON-NLS-1$
		}
		else {
			innerCond = String.format("%s < %s + %d", indexVar, newName, stripFactor); //$NON-NLS-1$
		}
		innerIter = loop.getIterationExpression().getRawSignature();
		innerBody = loop.getBody().getRawSignature();
		if (!(loop.getBody() instanceof IASTCompoundStatement)) {
			IASTComment[] outerComments = getBodyComments(loop);
			for (IASTComment comment : outerComments) {
				innerBody = comment.getRawSignature() + "\n" + innerBody; //$NON-NLS-1$
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
		outerInit = String.format("%s %s = %s", initType, newName, initRhs); //$NON-NLS-1$
		outerCond = String.format("%s %s %s", newName, compOp, ub); //$NON-NLS-1$
		outerIter = String.format("%s += %d", newName, stripFactor); //$NON-NLS-1$
		outer = forLoop(outerInit, outerCond, outerIter, compound(inner));
		this.replace(loop, outer);
		finalizeChanges();
	}

	protected String getOperatorAsString(IASTBinaryExpression condExpr) {
		String compOp;
		switch (condExpr.getOperator()) {
		case IASTBinaryExpression.op_lessEqual:
			compOp = "<="; //$NON-NLS-1$
			break;
		case IASTBinaryExpression.op_lessThan:
			compOp = "<"; //$NON-NLS-1$
			break;
		default:
			throw new IllegalStateException();
		}
		return compOp;
	}
	
}
