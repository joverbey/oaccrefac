package org.eclipse.ptp.pldt.openacc.core.transformations;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class IntroduceAtomicsCheck extends SourceStatementsCheck<RefactoringParams> {

    public IntroduceAtomicsCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
        super(statements, statementsAndComments);
    }

    /**
     * Here, we need to ensure that the variable that should become atomic is inside the parallel region and that the
     * declaration of that variable is outside of the parallel region.
     * 
     * For now, we're only going to care about the first statement in the list of statements given to us.
     */
	@Override
	protected void doReachingDefinitionsCheck(RefactoringStatus status, ReachingDefinitions rd) {
		// Find all of the pragmas surrounding the highlighted statements.
		populateAccMap();
		if (getStatements().length == 0) {
			// Failed because there weren't any statements to analyze.
			return;
		}

		IASTStatement statement = getStatements()[0];
		
		for (IASTPreprocessorStatement s : ASTUtil.getEnclosingPragmas(statement)) {
			System.out.println(s.getClass().getName());
		}
		
		
		boolean isInParallelRegion = false;
		for (IAccConstruct construct : getAccRegions().get(statement).values()) {
			if (construct instanceof ASTAccParallelLoopNode || construct instanceof ASTAccParallelNode 
					|| construct instanceof ASTAccKernelsNode) {
				isInParallelRegion = true;
			}
		}
		if (!isInParallelRegion) {
			System.out.println("Not in parallel region");
			// Not good. Need to error out.
		}
	}

}
