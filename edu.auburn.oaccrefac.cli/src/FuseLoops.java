import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.FuseLoopsCheck;
import edu.auburn.oaccrefac.core.transformations.FuseLoopsAlteration;

/**
 * Command line driver to fuse loops.
 */
public class FuseLoops extends Main<RefactoringParams, FuseLoopsCheck, FuseLoopsAlteration> {
    
    public static void main(String[] args) {
        new FuseLoops().run(args);
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
        System.err.println("Usage: FuseLoops <filename.c>");
    }

    @Override
    protected FuseLoopsCheck createCheck(IASTForStatement loop) {
        return new FuseLoopsCheck(loop);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected FuseLoopsAlteration createAlteration(IASTRewrite rewriter, FuseLoopsCheck check) throws CoreException {
        return new FuseLoopsAlteration(rewriter, check);
    }
    
}
