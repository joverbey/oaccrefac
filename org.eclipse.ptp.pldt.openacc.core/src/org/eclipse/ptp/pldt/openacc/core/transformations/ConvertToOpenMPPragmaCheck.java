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

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class ConvertToOpenMPPragmaCheck extends SourceFileCheck<InterchangeLoopParams> {

	private Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> pragmas;

	public ConvertToOpenMPPragmaCheck(Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> pragmas) {
		super(pragmas);
		this.pragmas = pragmas;
	}

	@Override
	protected void doPragmaFormCheck(RefactoringStatus status) {
		checkPragma(status);
	}

	private void checkPragma(RefactoringStatus status) {
		IASTPreprocessorIncludeStatement[] includeStmts = getTranslationUnit().getIncludeDirectives();
		for (IASTPreprocessorIncludeStatement include : includeStmts) {
			if (include.getName().toString().equalsIgnoreCase("openacc.h")) {
				status.addFatalError("Compiler doesn't support Openacc. Please remove it manually.");
			}
		}
		
		if(isOpenACCFunction(getTranslationUnit()))
			status.addFatalError("Compiler doesn't support OpenACC function. Please remove or change acc function to OpenMP.");
		
		//TODO check if there are two pragmas before one statement
		for (IASTPreprocessorPragmaStatement pragma : pragmas.keySet()) { //remove kernels clause
			boolean isKernel = false;
			if (pragma.getRawSignature().contains("kernels")) {
				pragmas.remove(pragma);
				status.addFatalError("Refactoring does not handle kernel clause. Please refactor it manually.");
			}
			else if (pragmas.get(pragma) != null) { //check loop nest for kernels clause
				for (int i = 1; i < pragmas.get(pragma).size(); i++) {
					ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(pragmas.get(pragma).get(i));
					for (IASTPreprocessorPragmaStatement p : inq.getLeadingPragmas()) {
						if (p.getRawSignature().contains("kernels")) {
							pragmas.remove(pragma);
							isKernel = true;
							break;
						}
					}
					if (isKernel) {
						status.addFatalError("Refactoring does not handle kernel clause in loop nest. Please refactor the loop manually.");
					}
				}
			}
		}
		
		for (IASTPreprocessorPragmaStatement pragma : pragmas.keySet()) { //check gang loop nested in vector or two gang loops
			boolean isVector = false;
			boolean isGang = false;
			
			if (pragma.getRawSignature().contains("gang"))
				isGang = true;
			
			if (pragma.getRawSignature().contains("vector"))
				isVector = true;
			
			if (pragmas.get(pragma) != null) {
				for (int i = 1; i < pragmas.get(pragma).size(); i++) {
					ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(pragmas.get(pragma).get(i));
					for (IASTPreprocessorPragmaStatement p : inq.getLeadingPragmas()) {
						if(isVector && p.getRawSignature().contains("gang"))
							status.addFatalError("Gang Loop nested within Vector loop is not supported.");
						
						if (isGang && p.getRawSignature().contains("gang"))
							status.addFatalError("Gang Loop nested within Gang loop is not supported.");						
						
						if (!isVector && p.getRawSignature().contains("vector"))
							isVector = true;
						
						if (!isGang && p.getRawSignature().contains("gang"))
							isGang = true;
					}
				}
			}
		}
	}

	private boolean isOpenACCFunction(IASTTranslationUnit tu) {
		List<IASTFunctionCallExpression> expressions = ASTUtil.find(tu, IASTFunctionCallExpression.class);
		for (IASTFunctionCallExpression exp : expressions) {
			if (exp.getFunctionNameExpression().getRawSignature().startsWith("acc_", 0)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> getPragmas() {
		return pragmas;
	}

	@Override
	public IASTTranslationUnit getTranslationUnit() {
		return pragmas.keySet().iterator().next().getTranslationUnit();
	}
}
