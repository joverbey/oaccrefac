package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;

public class IntroduceAtomicsCheck extends SourceStatementsCheck<RefactoringParams> {

    public IntroduceAtomicsCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
        super(statements, statementsAndComments);
    }

	@Override
	protected void doReachingDefinitionsCheck(RefactoringStatus status, ReachingDefinitions rd) {
		// TODO Auto-generated method stub
		
	}

}
