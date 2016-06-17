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
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class ConvertToOpenMPPragmaAlteration extends SourceFileAlteration<ConvertToOpenMPPragmaCheck> {

	private Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> pragmas;

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
	private static final String SEQUENTIAL_LOOP_TYPE = "seq";

	public ConvertToOpenMPPragmaAlteration(IASTRewrite rewriter, ConvertToOpenMPPragmaCheck check) {
		super(rewriter, check);
		this.pragmas = check.getPragmas();
	}

	@Override
	protected void doChange() throws Exception {
		for (IASTPreprocessorPragmaStatement pragma : pragmas.keySet()) {
			convertToOpenMP(pragma);
		}
		finalizeChanges();
	}

	private void convertToOpenMP(IASTPreprocessorPragmaStatement accPragma) {
		StringBuilder newPragma = new StringBuilder();
		if (accPragma.getRawSignature().contains(OPENACC_LOOP_DIRECTIVE)) {
			String ompPragma = handleLoop(accPragma);
			if (ompPragma == null)
				this.remove(accPragma);
		}

		else if (accPragma.getRawSignature().contains(PARALLEL_DIRECTIVE)) {
			newPragma.append(OPENMP_DIRECTIVE + " ");
			newPragma.append(handleParallelRegion(accPragma));
			this.replace(accPragma, pragma(newPragma.toString()));
		}

		else if (accPragma.getRawSignature().contains(OPENACC_DATA_DIRECTIVE)) {
			newPragma.append(OPENMP_DIRECTIVE + " ");
			newPragma.append(handleDataTransfer(accPragma));
			this.replace(accPragma, pragma(newPragma.toString()));
		}
	}

	private String handleLoop(IASTPreprocessorPragmaStatement pragma) {
		StringBuilder loopPragma = new StringBuilder();
		boolean isPragmaInLoop = false;
		boolean isOutermostLoop = true;

		if (pragma.getRawSignature().contains(SEQUENTIAL_LOOP_TYPE))
			return null;
		
		else {
			loopPragma.append(OPENMP_DIRECTIVE + " ");
			List<IASTForStatement> loops = pragmas.get(pragma);
			int loopCountWithNoPragma = 0;
			int nestingDepth = loops.size();
			for (IASTForStatement loop : loops) {
				ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(loop);
				List<IASTPreprocessorPragmaStatement> pragmasInLoop = inq.getLeadingPragmas();
				if (pragmasInLoop.isEmpty()) {
					isPragmaInLoop = true;
					loopCountWithNoPragma++;
				}

				for (IASTPreprocessorPragmaStatement p : pragmasInLoop) {
					loopPragma.append(handleGangAndVectorLoop(p, nestingDepth, isOutermostLoop));
					loopPragma.append(handleDataClause(p));
					String parallel = handleParallelClause(p, loop, nestingDepth, isOutermostLoop);
					if (parallel != null) {
						loopPragma.append(parallel);
						if (!isOutermostLoop && nestingDepth > 1) {
							IASTForStatement innerFor = loops.get(loops.size() - (nestingDepth - 1));
							ForStatementInquisitor innerInq = ForStatementInquisitor.getInquisitor(innerFor);
							List<IASTPreprocessorPragmaStatement> pragmasInInnerLoop = innerInq.getLeadingPragmas();
							if (!pragmasInInnerLoop.isEmpty()) {
								if (pragmasInInnerLoop.get(0).getRawSignature().contains("acc loop")
										&& !(pragmasInInnerLoop.get(0).getRawSignature().contains(SEQUENTIAL_LOOP_TYPE))) {
									this.remove(p);
									loopPragma = new StringBuilder();
									loopPragma.append(OPENMP_DIRECTIVE + " ");
									isPragmaInLoop = false;
									continue;
								}
							}
						}

						this.replace(p, pragma(loopPragma.toString()));
					}
					else {
						if (p.getRawSignature().contains(SEQUENTIAL_LOOP_TYPE) && nestingDepth ==1 && !isOutermostLoop) {
							IASTForStatement vectorLoop = null;
							
							for (int i = loops.size() - 2; i > 0; i--) {
								IASTForStatement outerFor = loops.get(loops.size() - (nestingDepth + 1));
								ForStatementInquisitor outerInq = ForStatementInquisitor.getInquisitor(outerFor);
								List<IASTPreprocessorPragmaStatement> pragmasInOuterLoop = outerInq.getLeadingPragmas();
								if (pragmasInOuterLoop.isEmpty()) {
									vectorLoop = outerFor;
									continue;
								}
								else if (!pragmasInOuterLoop.isEmpty() && pragmasInOuterLoop.get(0).getRawSignature().contains(SEQUENTIAL_LOOP_TYPE)) {
									continue;
								}
								else if (!pragmasInOuterLoop.isEmpty() && !pragmasInOuterLoop.get(0).getRawSignature().contains(SEQUENTIAL_LOOP_TYPE)) {
									break;
								}
							}
							if (vectorLoop != null) {
								StringBuilder innerPragma = new StringBuilder();
								innerPragma.append(OPENMP_DIRECTIVE + " " + PARALLEL_DIRECTIVE + " " + OPENMP_LOOP_DIRECTIVE);

								this.insertBefore(vectorLoop, pragma(innerPragma.toString()) + NL);
							}
							
						}	
					}

					loopPragma = new StringBuilder();
					loopPragma.append(OPENMP_DIRECTIVE + " ");
					isPragmaInLoop = false;
				}
				nestingDepth--;
				isOutermostLoop = false;
			}
			if (isPragmaInLoop && loopCountWithNoPragma == loops.size() - 1) {
				StringBuilder innerPragma = new StringBuilder();
				innerPragma.append(OPENMP_DIRECTIVE + " " + PARALLEL_DIRECTIVE + " " + OPENMP_LOOP_DIRECTIVE);

				IASTForStatement innerFor = loops.get(loops.size() - 1);
				this.insertBefore(innerFor, pragma(innerPragma.toString()) + NL);
			}

		}
		return loopPragma.toString();
	}

	private String handleParallelRegion(IASTPreprocessorPragmaStatement pragma) {
		StringBuilder newPragma = new StringBuilder();
		
		if (pragma.getRawSignature().contains(PARALLEL_DIRECTIVE))
			newPragma.append(OPENMP_TARGET_DIRECTIVE + " " + OPENMP_TEAMS_DIRECTIVE + " ");

		return newPragma.toString();
	}

	private String handleGangAndVectorLoop(IASTPreprocessorPragmaStatement pragma, int depth, boolean isOuter) {
		StringBuilder newPragma = new StringBuilder();
		boolean isAllSequential = false;
		if (isOuter && depth > 1) {
			isAllSequential = checkInnerSequentialLoop(pragma);
		}
	
		if (pragma.getRawSignature().contains(PARALLEL_DIRECTIVE)) {
			newPragma.append(OPENMP_TARGET_DIRECTIVE + " " + OPENMP_TEAMS_DIRECTIVE + " ");	
		}
			
		if (pragma.getRawSignature().contains("gang vector"))
			newPragma.append(OPENMP_DISTRIBUTE_DIRECTIVE + " " + PARALLEL_DIRECTIVE + " " + OPENMP_LOOP_DIRECTIVE + " ");

		else if (pragma.getRawSignature().contains("gang"))
			newPragma.append(OPENMP_DISTRIBUTE_DIRECTIVE + " ");

		else if (depth == 1 && isOuter) //gang vector
			newPragma.append(OPENMP_DISTRIBUTE_DIRECTIVE + " " + PARALLEL_DIRECTIVE + " " + OPENMP_LOOP_DIRECTIVE + " ");
		
		else if (depth > 1 && isOuter && isAllSequential) //gang vector
			newPragma.append(OPENMP_DISTRIBUTE_DIRECTIVE + " " + PARALLEL_DIRECTIVE + " " + OPENMP_LOOP_DIRECTIVE + " ");
		
		else if (depth > 1 && isOuter) //gang
			newPragma.append(OPENMP_DISTRIBUTE_DIRECTIVE + " ");
		
		if (pragma.getRawSignature().contains(OPENACC_LOOP_DIRECTIVE) && !isOuter) //vector
			newPragma.append(PARALLEL_DIRECTIVE + " " + OPENMP_LOOP_DIRECTIVE + " ");
		
		return newPragma.toString();
	}
	
	private boolean checkInnerSequentialLoop(IASTPreprocessorPragmaStatement pragma) {
		List<IASTForStatement> loops = pragmas.get(pragma);
		for (int i = 1; i < loops.size(); i++) {
			ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(loops.get(i));
			List<IASTPreprocessorPragmaStatement> pragmasInLoop = inq.getLeadingPragmas();
			if (!pragmasInLoop.isEmpty()) {
				if (!pragmasInLoop.get(0).getRawSignature().contains(SEQUENTIAL_LOOP_TYPE)) {
					return false;
				}
			}
			else
				return false;
		}
		return true;
	}

	private String handleParallelClause(IASTPreprocessorPragmaStatement pragma, IASTForStatement loop, int depth, boolean isOutermostLoop) {
		StringBuilder newPragma = new StringBuilder();

		if (pragma.getRawSignature().contains(OPENACC_GANGS_DIRECTIVE)) {
			newPragma.append(OPENMP_GANGS_DIRECTIVE + "(" + getValue(pragma, OPENACC_GANGS_DIRECTIVE) + ") ");
		}

		if (pragma.getRawSignature().contains(OPENACC_VECTOR_DIRECTIVE)) {
			newPragma.append(OPENMP_THREAD_DIRECTIVE + "(" + getValue(pragma, OPENACC_VECTOR_DIRECTIVE) + ") ");
		}

		if (pragma.getRawSignature().contains(PRIVATE_DIRECTIVE)) {
			handlePrivate(pragma, loop);
			if ((depth > 1 && !isOutermostLoop) || pragma.getRawSignature().contains(SEQUENTIAL_LOOP_TYPE)) {
				this.remove(pragma);
				return null;
			}
		}

		if (pragma.getRawSignature().contains(REDUCTION_DIRECTIVE)) {
			if (pragma.getRawSignature().contains(SEQUENTIAL_LOOP_TYPE)) {
				this.remove(pragma);
				return null;
			}
			String data = getValue(pragma, REDUCTION_DIRECTIVE);
			newPragma.append(REDUCTION_DIRECTIVE + "(" + data + ")");
		}

		if (pragma.getRawSignature().contains(SEQUENTIAL_LOOP_TYPE)) {
			this.remove(pragma);
			return null;
		}
		
		return newPragma.toString();
	}

	private String handlePrivate(IASTPreprocessorPragmaStatement accPragma, IASTForStatement loop) {
		StringBuilder newPragma = new StringBuilder();
		String privateVariable = getValue(accPragma, PRIVATE_DIRECTIVE);
		String[] privateVariableList = privateVariable.split(",");
		for (String var : privateVariableList) {
			List<IASTExpression> expressions = ASTUtil.find(loop, IASTExpression.class);
			for (IASTExpression expression : expressions) {
				if (expression instanceof IASTBinaryExpression) {
					IASTBinaryExpression expr = (IASTBinaryExpression) expression;
					if (expr.getOperand1() instanceof IASTIdExpression) {
						IASTIdExpression name = (IASTIdExpression) expr.getOperand1();
						if (name.getName().getRawSignature().equals(var.trim())) {
							String s = expr.getExpressionType().toString() + " " + var + ";";
							this.insertBefore(expr, s);
							break;
						}
					}
				}
			}
		}
		
		return newPragma.toString();
	}

	private String handleDataTransfer(IASTPreprocessorPragmaStatement accPragma) {
		StringBuilder newPragma = new StringBuilder();
		newPragma.append(OPENMP_DATA_DIRECTIVE + " ");
		newPragma.append(handleDataClause(accPragma));
		return newPragma.toString();
	}

	private String handleDataClause(IASTPreprocessorPragmaStatement accPragma) {
		StringBuilder newPragma = new StringBuilder();
		String copyPattern = "copy(";
		String copyInPattern = "copyin(";
		String copyOutPattern = "copyout(";
		String createPattern = "create(";
		String pragma = accPragma.getRawSignature();

		if (pragma.contains(copyPattern)) {
			String data = getValue(accPragma, copyPattern);
			newPragma.append("map(tofrom:" + data + ") ");
		}

		if (pragma.contains(copyInPattern)) {
			String data = getValue(accPragma, copyInPattern);
			newPragma.append("map(to:" + data + ") ");
		}

		if (pragma.contains(copyOutPattern)) {
			String data = getValue(accPragma, copyOutPattern);
			newPragma.append("map(from:" + data + ") ");
		}

		if (pragma.contains(createPattern)) {
			String data = getValue(accPragma, createPattern);
			newPragma.append("map(alloc:" + data + ") ");
		}

		return newPragma.toString();
	}

	private String getValue(IASTPreprocessorPragmaStatement accPragma, String directive) {
		String value = "";
		String pragma = accPragma.getRawSignature();
		int index = pragma.indexOf(directive);
		int start = pragma.indexOf("(", index);
		int end = pragma.indexOf(")", start);
		value = pragma.substring(start + 1, end);
		return value;
	}
}
