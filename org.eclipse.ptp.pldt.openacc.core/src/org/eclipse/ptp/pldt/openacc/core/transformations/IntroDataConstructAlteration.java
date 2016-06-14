/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopy;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyin;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyout;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCreate;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
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
        IASTStatement[] stmts = getStatements();
        ReachingDefinitions rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(stmts[0], IASTFunctionDefinition.class));
        
        InferCopyin inferCopyin = new InferCopyin(rd, getStatements());
        InferCopyout inferCopyout = new InferCopyout(rd, getStatements());
        InferCopy inferCopy = new InferCopy(inferCopyin, inferCopyout);
        InferCreate inferCreate = new InferCreate(rd, getStatements());
        
        StringBuilder newOuterPragma = new StringBuilder(pragma("acc data"));
        if(!inferCopyin.get().get(inferCopyin.getRoot()).isEmpty()) {
        	newOuterPragma.append(" ");
        	newOuterPragma.append(copyin(inferCopyin.get().get(inferCopyin.getRoot())));
        }
        if(!inferCopyout.get().get(inferCopyout.getRoot()).isEmpty()) {
        	newOuterPragma.append(" ");
            newOuterPragma.append(copyout(inferCopyout.get().get(inferCopyout.getRoot())));
        }
        if(!inferCopy.get().get(inferCopy.getRoot()).isEmpty()) {
        	newOuterPragma.append(" ");
            newOuterPragma.append(copyout(inferCopy.get().get(inferCopy.getRoot())));
        }
        if(!inferCreate.get().get(inferCreate.getRoot()).isEmpty()) {
        	newOuterPragma.append(" ");
            newOuterPragma.append(create(inferCreate.get().get(inferCreate.getRoot())));
        }
        
        replaceContainedPragmas(inferCopyin, inferCopyout, inferCopy, inferCreate);
        
        this.insertBefore(getAllEnclosedNodes()[0], newOuterPragma.toString() + NL + LCURLY);
        this.insertAfter(getAllEnclosedNodes()[getAllEnclosedNodes().length - 1], RCURLY);

        finalizeChanges();
    }
    
    private void replaceContainedPragmas(InferCopyin inferCopyin, InferCopyout inferCopyout, InferCopy inferCopy, InferCreate inferCreate) {
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
						sb.append(pragma("acc data"));
					} else if (construct instanceof ASTAccKernelsNode) {
						sb.append(pragma("acc kernels"));
					} else if (construct instanceof ASTAccKernelsLoopNode) {
						sb.append(pragma("acc kernels loop"));
					} else if (construct instanceof ASTAccParallelNode) {
						sb.append(pragma("acc parallel"));
					} else if (construct instanceof ASTAccParallelLoopNode) {
						sb.append(pragma("acc parallel loop"));
					}
					if(!inferCopyin.get().get(statement).isEmpty())
						sb.append(" " + copyin(inferCopyin.get().get(statement)));
					if(!inferCopyout.get().get(statement).isEmpty())
						sb.append(" " + copyout(inferCopyout.get().get(statement)));
					if(!inferCopy.get().get(statement).isEmpty())
						sb.append(" " + copy(inferCopy.get().get(statement)));
					if(!inferCreate.get().get(statement).isEmpty())
						sb.append(" " + create(inferCreate.get().get(statement)));
					this.replace(prag, sb.toString());
				} 
			}
    	}
    }
}
