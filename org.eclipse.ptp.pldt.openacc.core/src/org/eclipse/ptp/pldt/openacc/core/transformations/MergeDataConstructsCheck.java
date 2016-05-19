package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccDataNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
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
    	OpenACCParser parser = new OpenACCParser();
    	try {
           if(!(parser.parse(getPragma().getRawSignature()) instanceof ASTAccDataNode)) {
        	   status.addFatalError("The pragma must be a data construct.");
               return;
           }
        }
        catch(Exception e) {
            status.addFatalError("The pragma must be a data construct.");
            return;
        }
        
        IASTNode next = ASTUtil.getNextSibling(getStatement());
        if(next == null || !(next instanceof IASTStatement)) {
        	status.addFatalError("The data construct must be immediately followed by another data construct.");
        	return;
        }
        
        secondStmt = (IASTStatement) next;
        IAccConstruct second = null;
        for(IASTPreprocessorPragmaStatement prag : ASTUtil.getLeadingPragmas((IASTStatement) next)) {
        	IAccConstruct nextCon = null;
        	try {
        		nextCon = parser.parse(prag.getRawSignature());
        	}
        	catch(Exception e) {
        		continue;
        	}
        	if(nextCon instanceof ASTAccDataNode) {
        		second = (ASTAccDataNode) nextCon;
        		secondPrag = prag;
        		break;
        	}
        }
        
        if(second == null) {
        	status.addFatalError("The data construct must be immediately followed by another data construct.");
        	return;
        }
        
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
