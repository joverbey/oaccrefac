package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;

public class Expand extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

	public Expand(IASTRewrite rewriter, ExpandDataConstructCheck check) {
		super(rewriter, check);
	}
	
	@Override
	public void doChange() {
		int maxup = getMaxInDirection(getStatement(), true);
		int maxdown = getMaxInDirection(getStatement(), false);
		int osize = ASTUtil.getStatementsIfCompound(getStatement()).length;
		List<Expansion> expansions = new ArrayList<Expansion>();

		for (int i = 0; i <= maxup; i++) {
			for (int j = 0; j <= maxdown; j++) {
				IASTStatement[] expStmts = getExpansionStatements(i, j, getStatement());
				// be sure we don't add a declaration to this inner scope if it is used in the outer scope
				if (ASTUtil.doesConstructContainAllReferencesToVariablesItDeclares(expStmts))
					expansions.add(new Expansion(expStmts, osize + i + j));
			}
		}

		Expansion largestexp = null;
		for (Expansion exp : expansions) {
			if (largestexp == null || exp.getSize() >= largestexp.getSize()) {
				largestexp = exp;
			}
		}

		String newConstruct = "";
		for (IASTStatement statement : largestexp.getStatements()) {
			for (IASTPreprocessorPragmaStatement pragma : ASTUtil.getPragmaNodes(statement)) {
				if (!pragma.equals(getPragma()))
					newConstruct += pragma.getRawSignature() + System.lineSeparator();
			}
			if (statement.equals(getStatement())) {
				newConstruct += decompound(statement.getRawSignature()) + System.lineSeparator();
			} else {
				newConstruct += statement.getRawSignature() + System.lineSeparator();
			}
		}

		newConstruct = getPragma().getRawSignature() + System.lineSeparator() + compound(newConstruct);

		this.remove(getPragma());

		IASTStatement[] exparr = largestexp.getStatements();
		int start = exparr[0].getFileLocation().getNodeOffset();
		int end = exparr[exparr.length - 1].getFileLocation().getNodeOffset()
				+ exparr[exparr.length - 1].getFileLocation().getNodeLength();
		int len = end - start;
		this.replace(start, len, newConstruct);
		finalizeChanges();
	}
	
	private int getMaxInDirection(IASTStatement statement, boolean up) {
		int i = 0;
		IASTNode next = statement;
		while (true) {
			next = up ? ASTUtil.getPreviousSibling(next) : ASTUtil.getNextSibling(next);
			if (next == null || (next instanceof IASTStatement && OpenACCUtil.isAccConstruct((IASTStatement) next))) {
				break;
			}
			i++;
		}
		return i;
	}

	private IASTStatement[] getExpansionStatements(int stmtsUp, int stmtsDown, IASTStatement original) {
		List<IASTStatement> statements = new ArrayList<IASTStatement>();
		statements.add(original);

		IASTStatement current = original;
		for (int i = 0; i < stmtsUp; i++) {
			// TODO type checking
			current = (IASTStatement) ASTUtil.getPreviousSibling(current);
			statements.add(0, current);
		}

		current = original;
		for (int j = 0; j < stmtsDown; j++) {
			// TODO type checking
			current = (IASTStatement) ASTUtil.getNextSibling(current);
			statements.add(current);
		}
		return statements.toArray(new IASTStatement[statements.size()]);
	}

	protected class Expansion {

		private IASTStatement[] statements;
		private int size;

		public Expansion(IASTStatement[] statements, int size) {
			this.size = size;
			this.statements = statements;
		}

		public IASTStatement[] getStatements() {
			return statements;
		}

		public int getSize() {
			return size;
		}

	}

}
