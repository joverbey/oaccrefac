package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.ui.refactorings.changes.StripMine;

@SuppressWarnings("restriction")
public class LoopStripMiningRefactoring extends ForLoopRefactoring {

    private int m_stripFactor;
    
    public LoopStripMiningRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        m_stripFactor = -1;
    }
    
    public boolean setStripFactor(int factor) {
        if (factor > 1) {
            m_stripFactor = factor;
            return true;
        }
        return false;
    }
    
    @Override
    protected void doCheckFinalConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        if (m_stripFactor <= 0) {
            initStatus.addFatalError("Strip factor invalid");
        }
        if (!checkBoundDivisibility()) {
            initStatus.addFatalError("Original loop iterator does not"
                    + "divide evenly into strip factor. Cannot refactor.");
        }
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {        
        StripMine stripmine_change = new StripMine(getLoop(), m_stripFactor);
        rewriter.replace(getLoop(), stripmine_change.change(), null);
    }
    
    /**
     * This method returns a boolean value depicting whether it is still
     * possible to continue refactoring based on the divisbility between the
     * original loop's incrementer and the strip factor. The factor must be
     * greater than, and divisible by the factor.
     * consequence of non-divisible factors.
     * @return -- true or false on whether refactoring may continue
     */
    private boolean checkBoundDivisibility() {
        IASTExpression iterator = getLoop().getIterationExpression();
        if (iterator instanceof IASTBinaryExpression) {
            IASTBinaryExpression bin = (IASTBinaryExpression) iterator;
            IASTExpression op2 = bin.getOperand2();
            if (op2 instanceof IASTLiteralExpression) {
                IASTLiteralExpression op2lit = (IASTLiteralExpression) op2;
                if (op2lit.getKind() == IASTLiteralExpression.lk_integer_constant) {
                    int value = Integer.parseInt(new String(op2lit.getValue()));
                    if (m_stripFactor % value != 0 || m_stripFactor <= value) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
}
