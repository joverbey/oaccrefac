package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.OpenACCUtil;

public class Expand extends PragmaDirectiveAlteration<ExpandDataConstructCheck> {

	public Expand(IASTRewrite rewriter, ExpandDataConstructCheck check) {
		super(rewriter, check);
	}
	
	@Override
	public void doChange() {
		ReachingDefinitions rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(getStatement(), IASTFunctionDefinition.class));

		int maxup = getMaxInDirection(getStatement(), true);
		int maxdown = getMaxInDirection(getStatement(), false);
		int osize;
		if (getStatement() instanceof IASTCompoundStatement) {
			osize = ((IASTCompoundStatement) getStatement()).getStatements().length;
		} else {
			osize = 1;
		}
		List<Expansion> expansions = new ArrayList<Expansion>();

		for (int i = 0; i <= maxup; i++) {
			for (int j = 0; j <= maxdown; j++) {
				IASTStatement[] expStmts = getExpansionStatements(i, j, getStatement());
				// be sure we don't add a declaration to this inner scope if it is used in the outer scope
				if (!expansionAddsDeclarationIllegally(expStmts, getStatement(), rd))
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
			for (IASTPreprocessorPragmaStatement pragma : ASTUtil.getLeadingPragmas(statement)) {
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
	

	private boolean expansionAddsDeclarationIllegally(IASTStatement[] expStmts, IASTStatement origStmt,
			ReachingDefinitions rd) {
		// get all variable declarations in the expansion but not in the original construct
		Set<IASTDeclarationStatement> decls = new HashSet<IASTDeclarationStatement>();
		for (IASTStatement stmt : expStmts) {
			if (!stmt.equals(origStmt)) {
				decls.addAll(ASTUtil.find(stmt, IASTDeclarationStatement.class));
			}
		}

		Set<IBinding> declaredVars = new HashSet<IBinding>();
		for (IASTDeclarationStatement declStmt : decls) {
			if(declStmt.getDeclaration() instanceof IASTSimpleDeclaration) {
				for(IASTDeclarator decl : ((IASTSimpleDeclaration) declStmt.getDeclaration()).getDeclarators()) {
					declaredVars.add(decl.getName().resolveBinding());
				}
			}
		}
		
		IASTFunctionDefinition func = ASTUtil.findNearestAncestor(origStmt, IASTFunctionDefinition.class);
		Set<IASTName> namesInConstruct = new HashSet<IASTName>();
		Set<IASTName> namesInFuncButNotConstruct = new HashSet<IASTName>();
		for(IASTStatement stmt : expStmts) {
			namesInConstruct.addAll(ASTUtil.find(stmt, IASTName.class));
		}
		namesInFuncButNotConstruct.addAll(ASTUtil.find(func, IASTName.class));
		namesInFuncButNotConstruct.removeAll(namesInConstruct);
		
		//if we are pulling in a declaration of a var that is used outside the construct, that declaration
		//is no longer in the scope or parent scope of any references outside the construct
		for(IBinding var : declaredVars) {
			for(IASTName outsideReference : namesInFuncButNotConstruct) {
				if(outsideReference.resolveBinding().equals(var)) {
					return true;
				}
			}
		}

		return false;
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
