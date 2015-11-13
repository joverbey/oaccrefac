import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingParams;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingCheck;
import edu.auburn.oaccrefac.core.transformations.LoopCuttingAlteration;

/**
 * Command line driver to introduce a kernels loop.
 */
public class LoopCutting extends Main<LoopCuttingParams, LoopCuttingCheck, LoopCuttingAlteration> {
    
    public static void main(String[] args) {
        new IntroduceKernelsLoop().run(args);
    }

    private int cutFactor = 0;
    
    @Override
    protected boolean checkArgs(String[] args) {
        if (args.length != 2) {
            printUsage();
            return false;
        }
        
        try {
            cutFactor = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            printUsage();
            return false;
        }
        
        return true;
    }

    private void printUsage() {
        System.err.println("Usage: LoopCutting <filename.c> <cut_factor>");
    }

    @Override
    protected LoopCuttingCheck createCheck(IASTForStatement loop) {
        return new LoopCuttingCheck(loop);
    }

    @Override
    protected LoopCuttingParams createParams(IASTForStatement forLoop) {
        return new LoopCuttingParams(cutFactor);
    }

    @Override
    protected LoopCuttingAlteration createAlteration(IASTRewrite rewriter, LoopCuttingCheck check) throws CoreException {
        return new LoopCuttingAlteration(rewriter, cutFactor, check);
    }
    
}
