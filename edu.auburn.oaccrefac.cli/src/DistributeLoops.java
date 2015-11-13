import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.DistributeLoopsCheck;
import edu.auburn.oaccrefac.core.transformations.DistributeLoopsAlteration;

/**
 * Command line driver to introduce a kernels loop.
 */
public class DistributeLoops extends Main<RefactoringParams, DistributeLoopsCheck, DistributeLoopsAlteration> {
    
    public static void main(String[] args) {
        new IntroduceKernelsLoop().run(args);
    }

    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 1) {
            printUsage();
            return false;
        }
        return true;
    }

    private void printUsage() {
        System.err.println("Usage: DistributeLoops <filename.c>");
    }

    @Override
    protected DistributeLoopsCheck createCheck(IASTForStatement loop) {
        return new DistributeLoopsCheck(loop);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected DistributeLoopsAlteration createAlteration(IASTRewrite rewriter, DistributeLoopsCheck check) throws CoreException {
        return new DistributeLoopsAlteration(rewriter, check);
    }
    
}
