package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class MergeDataConstructsCheck extends PragmaDirectiveCheck<NullParams> {

	private IASTPreprocessorPragmaStatement secondPrag;
	private IASTStatement secondStmt;
	
	public MergeDataConstructsCheck(IASTPreprocessorPragmaStatement pragma, IASTStatement statement) {
		super(pragma, statement);
	}
	
    @Override
    public void doFormCheck(RefactoringStatus status) {
    	if(!isDataPragma(getPragma())) {
    		status.addFatalError("The pragma must be a data construct");
    		return;
    	}
        
        IASTNode next = ASTUtil.getNextSibling(getStatement());
        if(next == null || !(next instanceof IASTStatement) || getDataPragma((IASTStatement) next) == null) {
        	status.addFatalError("The data construct must be immediately followed by another data construct");
        	return;
        }
        
        secondStmt = (IASTStatement) next;
        secondPrag = getDataPragma(secondStmt);
        
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
    
    public IASTStatement getFirstStatement() {
    	return getStatement();
    }
    
    public IASTStatement getSecondStatement() {
    	return secondStmt;
    }

}
