/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Auburn University - initial API and implementation
 *******************************************************************************/
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.MergeDataConstructsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.MergeDataConstructsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class MergeDataConstructs extends CLIRefactoring<RefactoringParams, MergeDataConstructsCheck> {

    @Override
    protected MergeDataConstructsCheck createCheck(IASTStatement statement) {
    	if(ASTUtil.getPragmaNodes(statement).size() > 0){
    		return new MergeDataConstructsCheck(ASTUtil.getPragmaNodes(statement).get(0), statement);
    	}
    	return null;
    }

    @Override
    public MergeDataConstructsAlteration createAlteration(IASTRewrite rewriter, MergeDataConstructsCheck check)
    		throws CoreException {
        return new MergeDataConstructsAlteration(rewriter, check);
    }

}
