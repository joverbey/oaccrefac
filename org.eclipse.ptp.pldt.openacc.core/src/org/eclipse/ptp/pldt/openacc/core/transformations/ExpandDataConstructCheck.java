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

import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyinInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyoutInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitionsAnalysis;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

public class ExpandDataConstructCheck extends PragmaDirectiveCheck<RefactoringParams> {

    private ASTAccDataNode construct;
    private IASTForStatement forParent = null;
    
    public ExpandDataConstructCheck(RefactoringStatus status, IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
        super(status, pragma, statement);
        IASTNode parent = statement.getParent();
        if(parent instanceof IASTForStatement && ((IASTForStatement) parent).getBody().equals(getStatement())) {
			forParent = (IASTForStatement) parent;
		} 
		else if (parent instanceof IASTCompoundStatement && parent.getParent() instanceof IASTForStatement
				&& ((IASTCompoundStatement) parent).getChildren().length == 1) {
			forParent = (IASTForStatement) parent.getParent();
		}
    }
    
    @Override
    public void doFormCheck() {
        try {
            construct = (ASTAccDataNode) (new OpenACCParser().parse(getPragma().getRawSignature()));
        }
        catch(Exception e) {
            //will enter on Exception from parser or ClassCastException if ACC non-data pragma
            status.addFatalError(Messages.ExpandDataConstructCheck_MustBeDataConstruct);
        }
        
    }
    
    private void doReachingDefinitionsCheck() {
        if(forParent != null) {
        	ReachingDefinitionsAnalysis rd = ReachingDefinitionsAnalysis.forFunction(ASTUtil.findNearestAncestor(getStatement(), IASTFunctionDefinition.class));
        	ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(forParent);
    		if(inq.isCountedLoop()) {
    			IBinding index = inq.getIndexVariable();
				IASTName initProblem = getAccTransferProblems(rd, forParent.getInitializerStatement(), getStatement(), index);
				IASTName condProblem = getAccTransferProblems(rd, forParent.getConditionExpression(), getStatement(), index);
				IASTName iterProblem = getAccTransferProblems(rd, forParent.getIterationExpression(), getStatement(), index);
        		if(initProblem != null && !initProblem.resolveBinding().equals(index)) {
        			//status.addError(String.format(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemNonIndexVar, initProblem.getRawSignature(), initProblem.getFileLocation().getStartingLineNumber()));
        			status.addError(NLS.bind(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemNonIndexVar, new Object[] { initProblem.getRawSignature(), initProblem.getFileLocation().getStartingLineNumber() }));
        		}
        		if(condProblem != null && !condProblem.resolveBinding().equals(index)) {
        			status.addError(NLS.bind(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemNonIndexVar, new Object[] { condProblem.getRawSignature(), condProblem.getFileLocation().getStartingLineNumber() }));
        		}
        		if(iterProblem != null && !iterProblem.resolveBinding().equals(index)) {
        			status.addError(NLS.bind(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemNonIndexVar, new Object[] { iterProblem.getRawSignature(), iterProblem.getFileLocation().getStartingLineNumber() }));
        		}
        		
        		if(initProblem != null && initProblem.resolveBinding().equals(index)) {
    				status.addWarning(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemIndexVar);
    			}
        		else if(condProblem != null && condProblem.resolveBinding().equals(index)) {
    				status.addWarning(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemIndexVar);
    			}
        		else if(iterProblem != null && iterProblem.resolveBinding().equals(index)) {
    				status.addWarning(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemIndexVar);
    			}
    		}
    		else {
    			IASTName initProblem = getAccTransferProblems(rd, forParent.getInitializerStatement(), getStatement());
        		IASTName condProblem = getAccTransferProblems(rd, forParent.getConditionExpression(), getStatement());
        		IASTName iterProblem = getAccTransferProblems(rd, forParent.getIterationExpression(), getStatement());
        		if(initProblem != null) {
        			status.addError(NLS.bind(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemNonIndexVar, new Object[] { initProblem.getRawSignature(), initProblem.getFileLocation().getStartingLineNumber() }));
        		}
        		if(condProblem != null) {
        			status.addError(NLS.bind(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemNonIndexVar, new Object[] { condProblem.getRawSignature(), condProblem.getFileLocation().getStartingLineNumber() }));
        		}
        		if(iterProblem != null) {
        			status.addError(NLS.bind(Messages.ExpandDataConstructCheck_PromoteDataTransferProblemNonIndexVar, new Object[] { iterProblem.getRawSignature(), iterProblem.getFileLocation().getStartingLineNumber() }));
        		}
    		}
    		
        }
    }
    
    @Override
    public RefactoringStatus performChecks(IProgressMonitor pm, RefactoringParams params) {
    	super.performChecks(pm, params);
    	if(status.hasFatalError()) {
    		return status;
    	}
    	doReachingDefinitionsCheck();
    	return status;
    }
    
   
    public ASTAccDataNode getConstruct() {
        return construct;
    }

	public IASTForStatement getForParent() {
		return forParent;
	}

	/**
	 * Returns a "accelerator transfer problem" variable for a node included in a 
	 * data construct expansion.
	 * 
	 * If expanding a data construct to include a new node and that node is a definition of 
	 * a copied-in variable, the copied-in value may become incorrect.
	 * Also, if including a new use of a copied-out variable, the value used there may changed.
	 * 
	 * If any problem is discovered involving unignored variables, returns the first such
	 *   problem variable.
	 * If no problem is discovered, returns null. 
	 * If the only discovered problems involve ignored variables, returns an ignored variable 
	 *   that would be a problem. 
	 */
	public static IASTName getAccTransferProblems(ReachingDefinitionsAnalysis rd, IASTNode next, IASTStatement original, IBinding... ignore) {
		//if a definition in the newly-included statement reaches the construct and defines a variable in the copyin set, stop
		IASTName bad = null;
		CopyinInference copyin = new CopyinInference(new IASTStatement[] { original }, original);
		CopyoutInference copyout = new CopyoutInference(new IASTStatement[] { original }, original);
		List<IBinding> ignores = Arrays.asList(ignore);
		for(IASTName def : rd.reachingDefinitions(original)) {
			if(ASTUtil.isAncestor(def, next) && copyin.get().get(copyin.getRoot()).contains(def.resolveBinding())) {
				if(ignores.contains(def.resolveBinding())) {
					bad = def;
				}
				else {
					return def;
				}
			}
		}
		for(IASTName use : rd.reachedUses(original)) {
			if(ASTUtil.isAncestor(use, next) && copyout.get().get(copyout.getRoot()).contains(use.resolveBinding())) {
				if(ignores.contains(use.resolveBinding())) {
					bad = use;
				}
				else {
					return use;
				}
			}
		}
		return bad;
	}
	
}
