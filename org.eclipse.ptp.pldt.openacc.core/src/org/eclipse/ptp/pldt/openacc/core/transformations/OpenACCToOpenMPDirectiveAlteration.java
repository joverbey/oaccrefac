/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class OpenACCToOpenMPDirectiveAlteration extends SourceFileAlteration<OpenACCToOpenMPDirectiveCheck> {

	private List<IASTPreprocessorPragmaStatement> pragmas;

	private static final String OPENMP_DIRECTIVE = "omp";
	private static final String OPENACC_LOOP_DIRECTIVE = "loop";
	private static final String OPENMP_LOOP_DIRECTIVE = "for";
	private static final String PARALLEL_DIRECTIVE = "parallel";
	private static final String OPENACC_DATA_DIRECTIVE = "data";
	private static final String OPENMP_DATA_DIRECTIVE = "target data";
	private static final String OPENMP_TARGET_DIRECTIVE = "target";
	private static final String OPENMP_TEAMS_DIRECTIVE = "teams";
	private static final String OPENMP_DISTRIBUTE_DIRECTIVE = "distribute";
	private static final String OPENACC_GANGS_DIRECTIVE = "num_gangs";
	private static final String OPENMP_GANGS_DIRECTIVE = "num_teams";
	private static final String OPENACC_VECTOR_DIRECTIVE = "vector_length";
	private static final String OPENMP_THREAD_DIRECTIVE = "thread_limit";
	private static final String PRIVATE_DIRECTIVE = "private";
	private static final String REDUCTION_DIRECTIVE = "reduction";

	public OpenACCToOpenMPDirectiveAlteration(IASTRewrite rewriter, OpenACCToOpenMPDirectiveCheck check) {
		super(rewriter, check);
		this.pragmas = check.getStatement();
	}

	@Override
	protected void doChange() throws Exception {
		for (IASTPreprocessorPragmaStatement pragma : pragmas) {
			convertToOpenMPClauses(pragma);
		}
		finalizeChanges();
	}

	public List<IASTPreprocessorPragmaStatement> getPragmas() {
		return pragmas;
	}

	private void convertToOpenMPClauses(IASTPreprocessorPragmaStatement pragma) {
		String pragmaValue = pragma.getRawSignature();
		if (pragmaValue.contains("loop")) {
			convertLoop(pragma);
		} else {
			convertPragma(pragma);
		}
	}

	private void convertLoop(IASTPreprocessorPragmaStatement p) {
		StringBuilder newPragma = new StringBuilder();
		String pragmaValue = p.getRawSignature();
		if (pragmaValue.contains("seq")) {
			this.remove(p);
		}
		else {
			newPragma.append(OPENMP_DIRECTIVE + " ");
			IASTForStatement outerFor = findForLoopWithPragma(p);
			ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(outerFor);
			List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
			if (headers.size() == 1) {
				if (pragmaValue.contains(PARALLEL_DIRECTIVE)) {
					newPragma.append(OPENMP_TARGET_DIRECTIVE + " " + OPENMP_TEAMS_DIRECTIVE + " ");
				}
				if (pragmaValue.contains(OPENACC_LOOP_DIRECTIVE)) {
					newPragma.append(OPENMP_DISTRIBUTE_DIRECTIVE + " " + PARALLEL_DIRECTIVE + " " + OPENMP_LOOP_DIRECTIVE + " ");
				}
				if (pragmaValue.contains("private")) {
					int index = pragmaValue.indexOf("private");
					int first = pragmaValue.indexOf('(', index);
					int last = pragmaValue.indexOf(')', index);
					String s = pragmaValue.substring(first + 1, last);
					newPragma.append("private (" + s + ") ");
				}
				if (pragmaValue.contains("independent")) {
					newPragma.append("");
				}
			} else {
				if (pragmaValue.contains(PARALLEL_DIRECTIVE)) {
					newPragma.append(OPENMP_TARGET_DIRECTIVE + " " + OPENMP_TEAMS_DIRECTIVE + " ");
				}
				if (pragmaValue.contains(OPENACC_LOOP_DIRECTIVE)) {
					newPragma.append(OPENMP_DISTRIBUTE_DIRECTIVE + " ");
				}
				if (pragmaValue.contains("private")) {
					int index = pragmaValue.indexOf("private");
					int first = pragmaValue.indexOf('(', index);
					int last = pragmaValue.indexOf(')', index);
					String s = pragmaValue.substring(first + 1, last);
					newPragma.append("private (" + s + ") ");
				}
				if (pragmaValue.contains("independent")) {
					newPragma.append("");
				}


				StringBuilder innerPragma = new StringBuilder();
				innerPragma.append(OPENMP_DIRECTIVE + " " + PARALLEL_DIRECTIVE + " " + OPENMP_LOOP_DIRECTIVE );

				int depth = headers.size();
				IASTForStatement innerFor = headers.get(depth -1);
				this.insertBefore(innerFor, pragma(innerPragma.toString()));
			}

			this.replace(p, pragma(newPragma.toString()));
		}
	}

	private void convertPragma(IASTPreprocessorPragmaStatement p) {
		StringBuilder newPragma = new StringBuilder();
		String copyPattern = "copy(";
		String copyInPattern = "copyin(";
		String copyOutPattern = "copyout(";
		String createPattern = "create(";
		String pragma = p.getRawSignature();
		String[] pragmaValue = p.getRawSignature().split(" ");
		newPragma.append(OPENMP_DIRECTIVE + " ");
		for (int i = 2; i < pragmaValue.length; i++) {
			if (pragmaValue[i].equals(OPENACC_DATA_DIRECTIVE)) {
				newPragma.append(OPENMP_DATA_DIRECTIVE + " ");
			} else if (pragmaValue[i].contains(copyPattern)) {
				String s = getCopyData(pragma, pragmaValue[i]);
				newPragma.append("map(tofrom:" + s + ") ");
			} else if (pragmaValue[i].contains(copyInPattern)) {
				String s = getCopyData(pragma, pragmaValue[i]);
				newPragma.append("map(to:" + s + ") ");
			} else if (pragmaValue[i].contains(copyOutPattern)) {
				String s = getCopyData(pragma, pragmaValue[i]);
				newPragma.append("map(from:" + s + ") ");
			} else if (pragmaValue[i].contains(createPattern)) {
				String s = getCopyData(pragma, pragmaValue[i]);
				newPragma.append("map(alloc:" + s + ") ");
			} else if (pragmaValue[i].equals(PARALLEL_DIRECTIVE)) {
				newPragma.append(OPENMP_TARGET_DIRECTIVE + " " + OPENMP_TEAMS_DIRECTIVE + " ");
			} else if (pragmaValue[i].contains(OPENACC_GANGS_DIRECTIVE + "(")) {
				String s = getCopyData(pragma, pragmaValue[i]);
				newPragma.append(OPENMP_GANGS_DIRECTIVE + "(" + s + ") ");
			} else if (pragmaValue[i].contains(OPENACC_VECTOR_DIRECTIVE + "(")) {
				String s = getCopyData(pragma, pragmaValue[i]);
				newPragma.append(OPENMP_THREAD_DIRECTIVE + "(" + s + ") ");
			} else if (pragmaValue[i].contains(PRIVATE_DIRECTIVE + "(")) {
				String s = getCopyData(pragma, pragmaValue[i]);
				newPragma.append(PRIVATE_DIRECTIVE + "(" + s + ") ");
			} else if (pragmaValue[i].contains(REDUCTION_DIRECTIVE + "(")) {
				String s = getCopyData(pragma, pragmaValue[i]);
				newPragma.append(REDUCTION_DIRECTIVE + "(" + s + ") ");
			}
		}

		this.replace(p, pragma(newPragma.toString()));
	}

	private String getCopyData(String pragma, String dataClause) {
		int index = pragma.indexOf(dataClause);
		int first = pragma.indexOf('(', index);
		int last = pragma.indexOf(')', index);
		String s = pragma.substring(first + 1, last);
		return s;
	}

	private IASTForStatement findForLoopWithPragma(IASTPreprocessorPragmaStatement pragma) {
		class ForLoopFinder extends ASTVisitor {
			IASTForStatement nearestFollowingStatement;
			int after;

			public ForLoopFinder(IASTPreprocessorPragmaStatement pragma) {
				shouldVisitStatements = true;
				after = pragma.getFileLocation().getNodeOffset() + pragma.getFileLocation().getNodeLength();
			}

			@Override
			public int visit(IASTStatement stmt) {
				if (stmt instanceof IASTForStatement) {
					if (stmt.getFileLocation().getNodeOffset() >= after) {
						if (nearestFollowingStatement == null || stmt.getFileLocation().getNodeOffset() < nearestFollowingStatement
								.getFileLocation().getNodeOffset()) {
							nearestFollowingStatement = (IASTForStatement) stmt;
						}
					}
				}
				return PROCESS_CONTINUE;
			}
		}
		ForLoopFinder finder = new ForLoopFinder(pragma);
		getTranslationUnit().accept(finder);
		return finder.nearestFollowingStatement;
	}
}
