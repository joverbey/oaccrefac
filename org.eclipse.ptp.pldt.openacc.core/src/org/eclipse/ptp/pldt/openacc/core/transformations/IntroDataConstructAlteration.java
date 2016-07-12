/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyinInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyoutInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CreateInference;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class IntroDataConstructAlteration extends SourceStatementsAlteration<IntroDataConstructCheck> {

    public IntroDataConstructAlteration(IASTRewrite rewriter, IntroDataConstructCheck check) {
        super(rewriter, check);
    }

    @Override
    protected void doChange() {
        CopyinInference inferCopyin = new CopyinInference(getStatements());
        CopyoutInference inferCopyout = new CopyoutInference(getStatements());
        CopyInference inferCopy = new CopyInference(inferCopyin, inferCopyout);
        CreateInference inferCreate = new CreateInference(getStatements());
        
        StringBuilder newOuterPragma = new StringBuilder(pragma("acc data")); //$NON-NLS-1$
        if(!inferCopyin.get().get(inferCopyin.getRoot()).isEmpty()) {
        	newOuterPragma.append(" "); //$NON-NLS-1$
        	newOuterPragma.append(copyin(inferCopyin.get().get(inferCopyin.getRoot())));
        }
        if(!inferCopyout.get().get(inferCopyout.getRoot()).isEmpty()) {
        	newOuterPragma.append(" "); //$NON-NLS-1$
            newOuterPragma.append(copyout(inferCopyout.get().get(inferCopyout.getRoot())));
        }
        if(!inferCopy.get().get(inferCopy.getRoot()).isEmpty()) {
        	newOuterPragma.append(" "); //$NON-NLS-1$
            newOuterPragma.append(copy(inferCopy.get().get(inferCopy.getRoot())));
        }
        if(!inferCreate.get().get(inferCreate.getRoot()).isEmpty()) {
        	newOuterPragma.append(" "); //$NON-NLS-1$
            newOuterPragma.append(create(inferCreate.get().get(inferCreate.getRoot())));
        }
        
        replaceContainedPragmas(inferCopyin, inferCopyout, inferCopy, inferCreate);
        
        this.insertBefore(getAllEnclosedNodes()[0], newOuterPragma.toString() + NL + LCURLY);
        this.insertAfter(getAllEnclosedNodes()[getAllEnclosedNodes().length - 1], RCURLY);

        finalizeChanges();
    }
    
    private void replaceContainedPragmas(CopyinInference inferCopyin, CopyoutInference inferCopyout, CopyInference inferCopy, CreateInference inferCreate) {
    	Set<IASTStatement> allStatements = new HashSet<IASTStatement>();
    	allStatements.addAll(inferCopyin.get().keySet());
    	allStatements.addAll(inferCopyout.get().keySet());
    	allStatements.addAll(inferCopy.get().keySet());
    	allStatements.addAll(inferCreate.get().keySet());
    	for(IASTStatement statement : allStatements) {
    		if (statement != inferCopyin.getRoot()) {
				for (IASTPreprocessorPragmaStatement prag : ASTUtil.getPragmaNodes(statement)) {
					StringBuilder sb = new StringBuilder();
					IAccConstruct construct = null;
					try {
						construct = new OpenACCParser().parse(prag.getRawSignature());
					} catch (Exception e) {
						continue;
					}
					//TODO also get clauses other than copyin/copyout/create from constructs here
					if (construct instanceof ASTAccDataNode) {
						sb.append(pragma("acc data")); //$NON-NLS-1$
					} else if (construct instanceof ASTAccKernelsNode) {
						sb.append(pragma("acc kernels")); //$NON-NLS-1$
					} else if (construct instanceof ASTAccKernelsLoopNode) {
						sb.append(pragma("acc kernels loop")); //$NON-NLS-1$
					} else if (construct instanceof ASTAccParallelNode) {
						sb.append(pragma("acc parallel")); //$NON-NLS-1$
					} else if (construct instanceof ASTAccParallelLoopNode) {
						sb.append(pragma("acc parallel loop")); //$NON-NLS-1$
					}
					if(!inferCopyin.get().get(statement).isEmpty())
						sb.append(" " + copyin(inferCopyin.get().get(statement))); //$NON-NLS-1$
					if(!inferCopyout.get().get(statement).isEmpty())
						sb.append(" " + copyout(inferCopyout.get().get(statement))); //$NON-NLS-1$
					if(!inferCopy.get().get(statement).isEmpty())
						sb.append(" " + copy(inferCopy.get().get(statement))); //$NON-NLS-1$
					if(!inferCreate.get().get(statement).isEmpty())
						sb.append(" " + create(inferCreate.get().get(statement))); //$NON-NLS-1$
					this.replace(prag, sb.toString());
				} 
			}
    	}
    }
}
