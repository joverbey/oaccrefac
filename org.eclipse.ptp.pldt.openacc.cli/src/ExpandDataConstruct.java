/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jacob Allen Neeley (Auburn) - initial API and implementation
 *******************************************************************************/
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.ExpandDataConstructAlteration;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.ExpandDataConstructCheck;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.internal.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class ExpandDataConstruct extends CLIRefactoring<RefactoringParams, ExpandDataConstructCheck> {

    @Override
    protected ExpandDataConstructCheck createCheck(IASTStatement statement) {
    	if(ASTUtil.getPragmaNodes(statement).size() > 0){
    		return new ExpandDataConstructCheck(new RefactoringStatus(), ASTUtil.getPragmaNodes(statement).get(0), statement);
    	}
    	return null;
    }

    @Override
    public ExpandDataConstructAlteration createAlteration(IASTRewrite rewriter, ExpandDataConstructCheck check)
    		throws CoreException {
        return new ExpandDataConstructAlteration(rewriter, check);
    }

}
