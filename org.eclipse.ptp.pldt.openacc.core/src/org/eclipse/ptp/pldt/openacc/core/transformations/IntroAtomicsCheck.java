package org.eclipse.ptp.pldt.openacc.core.transformations;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccKernelsNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelLoopNode;
import org.eclipse.ptp.pldt.openacc.core.parser.ASTAccParallelNode;
import org.eclipse.ptp.pldt.openacc.core.parser.IAccConstruct;
import org.eclipse.ptp.pldt.openacc.core.parser.OpenACCParser;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.AtomicStatementInquisitor;

public class IntroAtomicsCheck extends SourceStatementsCheck<RefactoringParams> {
	
	public static final int NONE = 0;
	public static final int READ = 1;
	public static final int WRITE = 2;
	public static final int UPDATE = 3;
	
	private int type = NONE;

    public IntroAtomicsCheck(IASTStatement[] statements, IASTNode[] statementsAndComments) {
    	super(statements, statementsAndComments);
    }

    /**
     * Here, we need to ensure that the variable that should become atomic is inside the parallel region and that the
     * declaration of that variable is outside of the parallel region.
     * 
     * For now, we're only going to care about the first statement in the list of statements given to us.
     */
	protected void checkAtomicsAvailable(RefactoringStatus status) {
		// Find all of the pragmas surrounding the highlighted statements.
		if (getStatements().length == 0) {
			// Failed because there weren't any statements to analyze.
			status.addFatalError("No statements selected");
			return;
		}
		IASTStatement statement = getStatements()[0];
		if (!(statement instanceof IASTExpressionStatement)) {
			status.addError("Statement must be an expression statement.");
			return;
		}
		if (statement.getParent() instanceof IASTForStatement) {
			IASTForStatement forLoop = (IASTForStatement) statement.getParent();
			if (!forLoop.getBody().equals(statement)) {
				// Note: This is probably using the default Object.equals() method, but that should be fine here
				// since we're pretty much just checking to see if the selected statement is the loop body. We could
				// probably even use ==, but I don't want to go too crazy.
				status.addFatalError("Statement cannot be inside a for loop declaration.");
				return;
			}
		}
		
        OpenACCParser parser = new OpenACCParser();
        Map<IAccConstruct, IASTNode> prags = new HashMap<>();
        Map<IASTPreprocessorPragmaStatement, IASTNode> enclosingPragmas = ASTUtil.getEnclosingPragmas(statement);
		for (IASTPreprocessorPragmaStatement pragma : enclosingPragmas.keySet()) {
            try {
                IAccConstruct con = parser.parse(pragma.getRawSignature());
                prags.put(con, enclosingPragmas.get(pragma));
            } catch(Exception e) {
                // Parser couldn't parse the pragma; it's probably not an OpenACC pragma.
            }
        }
		
		IASTNode parallelRegion = null;
		for (IAccConstruct construct : prags.keySet()) {
			if (construct instanceof ASTAccParallelLoopNode || construct instanceof ASTAccParallelNode 
					|| construct instanceof ASTAccKernelsNode || construct instanceof ASTAccKernelsLoopNode) {
				parallelRegion = prags.get(construct);
				break;
			}
		}

		if (parallelRegion == null) {
			status.addError("Statement is not in a parallel region.");
			return;
		}
		AtomicStatementInquisitor inquisitor = AtomicStatementInquisitor.newInstance(statement, parallelRegion); 
		switch (inquisitor.getType()) {
		case AtomicStatementInquisitor.WRITE:
			type = WRITE;
			break;
		case AtomicStatementInquisitor.READ:
			type = READ;
			break;
		case AtomicStatementInquisitor.UPDATE:
			type = UPDATE;
			break;
		case AtomicStatementInquisitor.NONE:
			status.addFatalError("No suitable atomic type found.");
			type = NONE;
			break;
		}
	}

	public int getType() {
		return type;
	}

	@Override
	public RefactoringStatus doCheck(RefactoringStatus status, IProgressMonitor pm) {
		checkAtomicsAvailable(status);
		return status;
	}

}
