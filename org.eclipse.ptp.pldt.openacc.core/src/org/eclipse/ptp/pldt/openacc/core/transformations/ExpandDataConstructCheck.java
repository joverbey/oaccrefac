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

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyin;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyout;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class ExpandDataConstructCheck extends PragmaDirectiveCheck<RefactoringParams> {

    private ASTAccDataNode construct;
    private IASTForStatement forParent = null;
    
    public ExpandDataConstructCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        super(pragma, statement);
    }
    
    @Override
    public void doFormCheck(RefactoringStatus status) {
        String msg = "The pragma must be a data construct";
        try {
            construct = (ASTAccDataNode) (new OpenACCParser().parse(getPragma().getRawSignature()));
        }
        catch(Exception e) {
            //will enter on Exception from parser or ClassCastException if ACC non-data pragma
            status.addFatalError(msg);
        }
        
    }
    
    private void doReachingDefinitionsCheck(RefactoringStatus status) {
    	IASTNode parent = getStatement().getParent();
        if(parent instanceof IASTForStatement && ((IASTForStatement) parent).getBody().equals(getStatement())) {
			forParent = (IASTForStatement) parent;
		} 
		else if (parent instanceof IASTCompoundStatement && parent.getParent() instanceof IASTForStatement
				&& ((IASTCompoundStatement) parent).getChildren().length == 1) {
			forParent = (IASTForStatement) parent.getParent();
		}
        if(forParent == null) {
        	ReachingDefinitions rd = new ReachingDefinitions(ASTUtil.findNearestAncestor(getStatement(), IASTFunctionDefinition.class));
    		if(!checkCopyinCopyoutReachingDefinitions(rd, forParent.getInitializerStatement(), getStatement())
    				|| !checkCopyinCopyoutReachingDefinitions(rd, forParent.getConditionExpression(), getStatement())
    				|| !checkCopyinCopyoutReachingDefinitions(rd, forParent.getIterationExpression(), getStatement())) {
    			status.addError("Construct will be promoted above its containing for loop, but doing so may change the values copied to or from the accelerator");
    		}
        }
    }
    
    public static boolean checkCopyinCopyoutReachingDefinitions(ReachingDefinitions rd, IASTNode next, IASTStatement original) {
		//if a definition in the newly-included statement reaches the construct and defines a variable in the copyin set, stop
		InferCopyin copyin = new InferCopyin(rd, new IASTStatement[] { original }, original);
		InferCopyout copyout = new InferCopyout(rd, new IASTStatement[] { original }, original);
		for(IASTName def : rd.reachingDefinitions(original)) {
			if(ASTUtil.isAncestor(def, next) && copyin.get().get(copyin.getRoot()).contains(def.resolveBinding())) {
				return false;
			}
		}
		for(IASTName use : rd.reachedUses(original)) {
			if(ASTUtil.isAncestor(use, next) && copyout.get().get(copyout.getRoot()).contains(use.resolveBinding())) {
				return false;
			}
		}
		return true;
	}
    
    @Override
    public RefactoringStatus performChecks(RefactoringStatus status, IProgressMonitor pm, RefactoringParams params) {
    	super.performChecks(status, pm, params);
    	if(status.hasFatalError()) {
    		return status;
    	}
    	doReachingDefinitionsCheck(status);
    	return status;
    }
    
    public ASTAccDataNode getConstruct() {
        return construct;
    }

	public IASTForStatement getForParent() {
		return forParent;
	}

}
