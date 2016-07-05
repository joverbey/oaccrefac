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

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
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

public class MergeDataConstructsAlteration extends PragmaDirectiveAlteration<MergeDataConstructsCheck> {

	private IASTPreprocessorPragmaStatement secondPrag;
	private IASTCompoundStatement secondStmt;
	
    public MergeDataConstructsAlteration(IASTRewrite rewriter, MergeDataConstructsCheck check) {
        super(rewriter, check);
        secondPrag = check.getSecondPragma();
        secondStmt = check.getSecondStatement();
    }

    @Override
    protected void doChange() {

    	IASTStatement[] statements = concat(ASTUtil.getStatementsIfCompound(getFirstStatement()), ASTUtil.getStatementsIfCompound(getSecondStatement()));
    	
    	CopyinInference inferCopyin = new CopyinInference(statements, getFirstStatement(), getSecondStatement());
    	CopyoutInference inferCopyout = new CopyoutInference(statements, getFirstStatement(), getSecondStatement());
    	CopyInference inferCopy = new CopyInference(inferCopyin, inferCopyout, getFirstStatement(), getSecondStatement());
    	CreateInference inferCreate = new CreateInference(statements, getFirstStatement(), getSecondStatement());
    	
    	remove(getSecondPragma());
    	removeCurlyBraces(getFirstStatement());
    	removeCurlyBraces(getSecondStatement());
    	
    	Set<IASTStatement> all = new HashSet<IASTStatement>();
    	all.addAll(inferCopyin.get().keySet());
    	all.addAll(inferCopyout.get().keySet());
    	all.addAll(inferCopy.get().keySet());
    	all.addAll(inferCreate.get().keySet());
    	
    	for(IASTStatement con : all) {
    		if(!con.equals(inferCopyin.getRoot())) {
    			//TODO: this does assume that a statement only has one OpenACC pragma on it - it may have more, even though thats a bad idea
    			for(IASTPreprocessorPragmaStatement pragma : ASTUtil.getPragmaNodes(con)) {
    				IAccConstruct ast = null;
    				try {
    					ast = new OpenACCParser().parse(pragma.getRawSignature());
    				}
    				catch(Exception e) {
    					continue;
    				}
    				String newPragma;
        			if(ast instanceof ASTAccDataNode) newPragma = pragma("acc data"); //$NON-NLS-1$
        			else if(ast instanceof ASTAccParallelNode) newPragma = pragma("acc parallel"); //$NON-NLS-1$
        			else if(ast instanceof ASTAccParallelLoopNode) newPragma = pragma("acc parallel loop"); //$NON-NLS-1$
        			else if(ast instanceof ASTAccKernelsNode) newPragma = pragma("acc kernels"); //$NON-NLS-1$
        			else if(ast instanceof ASTAccKernelsLoopNode) newPragma = pragma("acc kernels loop"); //$NON-NLS-1$
        			else throw new IllegalStateException();
        			if(!inferCopyin.get().get(con).isEmpty())
        				newPragma += " " + copyin(inferCopyin.get().get(con)); //$NON-NLS-1$
        			if(!inferCopyout.get().get(con).isEmpty())
        				newPragma += " " + copyout(inferCopyout.get().get(con)); //$NON-NLS-1$
        			if(!inferCopy.get().get(con).isEmpty())
        				newPragma += " " + copy(inferCopy.get().get(con)); //$NON-NLS-1$
        			if(!inferCreate.get().get(con).isEmpty())
        				newPragma += " " + create(inferCreate.get().get(con)); //$NON-NLS-1$
        			replace(pragma, newPragma);
    			}
    		}
    	}
    	IASTStatement con = inferCopyin.getRoot();
    	String top = pragma("acc data"); //$NON-NLS-1$
    	if(!inferCopyin.get().get(con).isEmpty())
    		top += " " + copyin(inferCopyin.get().get(con)); //$NON-NLS-1$
    	if(!inferCopyout.get().get(con).isEmpty())
    		top += " " + copyout(inferCopyout.get().get(con)); //$NON-NLS-1$
    	if(!inferCopy.get().get(con).isEmpty())
    		top += " " + copy(inferCopy.get().get(con)); //$NON-NLS-1$
    	if(!inferCreate.get().get(con).isEmpty())
    		top += " " + create(inferCreate.get().get(con)); //$NON-NLS-1$
    	replace(getFirstPragma(), top);
		insertBefore(getFirstStatement(), LCURLY);
		insertAfter(getSecondStatement(), RCURLY);

    	finalizeChanges();
    }

    private void removeCurlyBraces(IASTStatement statement) {
		if(statement instanceof IASTCompoundStatement) {
			int stmtOffset = statement.getFileLocation().getNodeOffset();
			String comp = statement.getRawSignature();
			this.remove(stmtOffset + comp.indexOf('{'), 1);
			this.remove(stmtOffset + comp.lastIndexOf('}'), 1);
		}
	}

	private IASTStatement[] concat(IASTStatement[] one, IASTStatement[] two) {
    	IASTStatement[] both = new IASTStatement[one.length + two.length];
    	System.arraycopy(one, 0, both, 0, one.length);
    	System.arraycopy(two, 0, both, one.length, two.length);
    	return both;
    }
    
    public IASTPreprocessorPragmaStatement getFirstPragma() {
        return getPragma();
    }
    
    public IASTPreprocessorPragmaStatement getSecondPragma() {
    	return secondPrag;
    }
    
    public IASTCompoundStatement getFirstStatement() {
    	return (IASTCompoundStatement) getStatement();
    }
    
    public IASTCompoundStatement getSecondStatement() {
    	return (IASTCompoundStatement) secondStmt;
    }

}