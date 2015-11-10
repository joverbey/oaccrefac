import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.IntroParallelCheck;
import edu.auburn.oaccrefac.core.transformations.IntroParallelAlteration;

/**
 * Command line driver to introduce a parallel loop.
 */
public class IntroduceParallelLoop extends Main<RefactoringParams, IntroParallelCheck, IntroParallelAlteration> {
    
    public static void main(String[] args) {
        new IntroduceParallelLoop().run(args);
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
        System.err.println("Usage: IntroduceParallelLoop <filename.c>");
    }

    @Override
    protected IntroParallelCheck createCheck(IASTForStatement loop) {
        return new IntroParallelCheck(loop);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected IntroParallelAlteration createAlteration(IASTRewrite rewriter, IntroParallelCheck check) throws CoreException {
        return new IntroParallelAlteration(rewriter, check);
    }
    
}
