import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.transformations.ExpandDataConstructAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.ExpandDataConstructCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class ExpandDataConstruct 
		extends CLIRefactoring<RefactoringParams, ExpandDataConstructCheck, ExpandDataConstructAlteration> {

    @Override
    protected ExpandDataConstructCheck createCheck(IASTStatement statement) {
        return new ExpandDataConstructCheck(ASTUtil.getLeadingPragmas(statement).get(0), statement);
    }

    @Override
    public ExpandDataConstructAlteration createAlteration(IASTRewrite rewriter, ExpandDataConstructCheck check)
    		throws CoreException {
        return new ExpandDataConstructAlteration(rewriter, check);
    }

}
