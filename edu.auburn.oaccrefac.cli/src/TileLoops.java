import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.IntroduceKernelsLoopCheck;
import edu.auburn.oaccrefac.core.transformations.IntroduceKernelsLoopAlteration;

/**
 * Command line driver to introduce a kernels loop.
 */
public class IntroduceKernelsLoop extends Main<RefactoringParams, IntroduceKernelsLoopCheck, IntroduceKernelsLoopAlteration> {
    
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
        System.err.println("Usage: IntroduceKernelsLoop <filename.c>");
    }

    @Override
    protected IntroduceKernelsLoopCheck createCheck(IASTForStatement loop) {
        return new IntroduceKernelsLoopCheck(loop);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected IntroduceKernelsLoopAlteration createAlteration(IASTRewrite rewriter, IntroduceKernelsLoopCheck check) throws CoreException {
        return new IntroduceKernelsLoopAlteration(rewriter, check);
    }
    
}
