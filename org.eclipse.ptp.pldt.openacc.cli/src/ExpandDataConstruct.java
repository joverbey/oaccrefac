import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.transformations.ExpandDataConstructAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.ExpandDataConstructCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class ExpandDataConstruct extends StatementMain<RefactoringParams, ExpandDataConstructCheck, ExpandDataConstructAlteration> {

    /**
     * main begins refactoring execution.
     * 
     * @param args Arguments to the refactoring.
     */
    public static void main(String[] args) {
        new ExpandDataConstruct().run(args);
    }

    @Override
    protected boolean checkArgs(String[] args) {
        return true;
    }

    @Override
    protected ExpandDataConstructCheck createCheck(IASTStatement statement) {
        return new ExpandDataConstructCheck(ASTUtil.getLeadingPragmas(statement).get(0), statement);
    }
    
    @Override
    protected RefactoringParams createParams(IASTStatement statement) {
        return null;
    }

    @Override
    protected ExpandDataConstructAlteration createAlteration(IASTRewrite rewriter, ExpandDataConstructCheck check) throws CoreException {
        return new ExpandDataConstructAlteration(rewriter, check);
    }

    
}
