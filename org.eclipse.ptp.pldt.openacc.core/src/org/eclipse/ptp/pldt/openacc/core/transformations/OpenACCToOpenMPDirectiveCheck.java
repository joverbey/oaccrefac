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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionCallExpression;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;


public class OpenACCToOpenMPDirectiveCheck extends SourceFileCheck<InterchangeLoopParams> {
    
    private List<IASTPreprocessorPragmaStatement> pragmaStmts;

    public OpenACCToOpenMPDirectiveCheck(List<IASTPreprocessorPragmaStatement> pragmas) {
        super(pragmas);
        this.pragmaStmts = pragmas;
    }
    
    @Override
    protected void doPragmaFormCheck(RefactoringStatus status) {
        checkPragma(status);
    }
    
    @SuppressWarnings("restriction")
	private void checkPragma(RefactoringStatus status) {
    	String[] pragmaValue;
        for (IASTPreprocessorPragmaStatement pragma : pragmaStmts) {
        	pragmaValue = pragma.getRawSignature().split(" ");
        	if (pragmaValue[2].equalsIgnoreCase("kernel")) {
        		pragmaStmts.remove(pragma);
        		status.addInfo("Refactoring does not handle kernel clause. Please refactor it manually.");
        	}
        }
        IASTPreprocessorIncludeStatement[] includeStmts = getTranslationUnit().getIncludeDirectives();
        for (IASTPreprocessorIncludeStatement include : includeStmts) {
        	if (include.getName().toString().equalsIgnoreCase("openacc.h")) {
        		status.addFatalError("Compiler doesn't support Openacc. Please remove it manually.");
        	}
        }
        List<CASTFunctionCallExpression> funcCalls = findAllFunctionCall(getTranslationUnit());
        for (CASTFunctionCallExpression func : funcCalls) {
        	String funcName = func.getFunctionNameExpression().getRawSignature();
        	if (funcName.startsWith("acc_", 0)) {
        		status.addFatalError("Compiler doesn't support OpenACC. Please remove or change acc function to OpenMP.");
        	}
        }
        
    }
    
    @SuppressWarnings("restriction")
	private List<CASTFunctionCallExpression> findAllFunctionCall(IASTTranslationUnit tu) {
   
            class FunctionCallFinder extends ASTVisitor {
            	List<CASTFunctionCallExpression> nearestFollowingStatement = new LinkedList<CASTFunctionCallExpression>();
                
                public FunctionCallFinder(IASTTranslationUnit tu) {
                	shouldVisitExpressions = true;
                }

				@Override
                public int visit(IASTExpression stmt) {
                    if (stmt instanceof CASTFunctionCallExpression) {
                            nearestFollowingStatement.add((CASTFunctionCallExpression) stmt);
                    }
                    return PROCESS_CONTINUE;
                }
            }
            FunctionCallFinder finder = new FunctionCallFinder(tu);
            tu.accept(finder);
            return finder.nearestFollowingStatement;
    }
    
    public List<IASTPreprocessorPragmaStatement> getStatement() {
        return pragmaStmts;
    }

    @Override
    public IASTTranslationUnit getTranslationUnit() {
        return pragmaStmts.get(0).getTranslationUnit();
    }
}
