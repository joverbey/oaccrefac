import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.IntroDefaultNoneCheck;
import edu.auburn.oaccrefac.core.transformations.IntroDefaultNoneAlteration;

/**
 * Command line driver to introduce a kernels loop.
 */
public class IntroduceDefaultNone extends Main<RefactoringParams, IntroDefaultNoneCheck, IntroDefaultNoneAlteration> {
    
    public static void main(String[] args) {
        new IntroduceDefaultNone().run(args);
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
        System.err.println("Usage: IntroduceDefaultNone <filename.c>");
    }

    @Override
    protected IntroDefaultNoneCheck createCheck(IASTForStatement loop) {
        return new IntroDefaultNoneCheck(loop);
    }

    @Override
    protected RefactoringParams createParams(IASTForStatement forLoop) {
        // RefactoringParams is abstract
        return null;
    }

    @Override
    protected IntroDefaultNoneAlteration createAlteration(IASTRewrite rewriter, IntroDefaultNoneCheck check) throws CoreException {
        return new IntroDefaultNoneAlteration(rewriter, check);
    }
    
}
