/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.transformations;

import java.util.Arrays;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.internal.core.parser.OpenACCParser;

public class MergeDataConstructsCheck extends PragmaDirectiveCheck<RefactoringParams> {

	private IASTPreprocessorPragmaStatement secondPrag;
	private IASTStatement secondStmt;
	
	public MergeDataConstructsCheck(RefactoringStatus status, IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
		super(status, pragma, statement);
	}
	
    @Override
    public void doFormCheck() {
    	if(!isDataPragma(getPragma())) {
    		status.addFatalError(Messages.MergeDataConstructsCheck_MustBeDataConstruct);
    		return;
    	}
        
        IASTNode next = ASTUtil.getNextSibling(getStatement());
        if(next == null || !(next instanceof IASTStatement) || getDataPragma((IASTStatement) next) == null) {
        	status.addFatalError(Messages.MergeDataConstructsCheck_MustBeFollowedByDataConstruct);
        	return;
        }
        
        if(!(getStatement() instanceof IASTCompoundStatement) || !(next instanceof IASTCompoundStatement)) {
        	status.addFatalError(Messages.MergeDataConstructsCheck_ShouldBeCompoundStatements);
        	return;
        }
        
        secondStmt = (IASTStatement) next;
        secondPrag = getDataPragma(secondStmt);
        
        IASTName conflict = getNameConflict(getFirstStatement(), getSecondStatement());
        if(conflict != null) {
        	status.addError(NLS.bind(Messages.MergeDataConstructsCheck_VariableShadowingMayOccur, new Object[] { conflict.getRawSignature(), conflict.getRawSignature() }));
            return;
        }
        
    }
    
    private boolean isDataPragma(IASTPreprocessorPragmaStatement pragma) {
    	OpenACCParser parser = new OpenACCParser();
    	try {
            if(!(parser.parse(pragma.getRawSignature()) instanceof ASTAccDataNode)) {
                return false;
            }
         }
         catch(Exception e) {
             return false;
         }
    	return true;
    }
    
    private IASTName getNameConflict(IASTCompoundStatement first, IASTCompoundStatement second) {
    	for(IASTDeclarator decl : ASTUtil.find(first, IASTDeclarator.class)) {
    		for(IASTName name : ASTUtil.find(second, IASTName.class)) {
    			if(decl.getName().getRawSignature().equals(name.getRawSignature())
    					&& varWillShadow(name.resolveBinding(), second)) {
    				return decl.getName();
    			}
    		}
    	}
    	return null;
    }
    
    private boolean varWillShadow(IBinding var, IASTCompoundStatement second) {
    	for(IASTDeclarator decl : ASTUtil.find(second, IASTDeclarator.class)) {
    		IASTDeclarationStatement declStmt = ASTUtil.findNearestAncestor(decl, IASTDeclarationStatement.class);
    		if(decl.getName().resolveBinding().equals(var)) {
    			if(Arrays.asList(second.getStatements()).contains(declStmt)) {
    				return true;
    			}
    			else {
    				return false;
    			}
    		}
    	}
		return true;
	}
    
	private IASTPreprocessorPragmaStatement getDataPragma(IASTStatement statement) {
    	for(IASTPreprocessorPragmaStatement prag : ASTUtil.getPragmaNodes((IASTStatement) statement)) {
    		if(isDataPragma(prag)) {
    			return prag;
    		}
    	}
    	return null;
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
