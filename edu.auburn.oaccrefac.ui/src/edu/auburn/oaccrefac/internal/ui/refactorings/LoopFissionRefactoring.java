package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * This class defines the implementation for re-factoring using loop fusion. For example:
 * 
 * ORIGINAL:                  REFACTORED: 
 * int i, a[100], b[100];     | int i, a[100], b[100]; 
 * for (i = 0; i < 100; i++) {| for (i = 0; i <100; i++)
 *    a[i] = 1;               |     a[i] = 1;
 *    b[i] = 2;               | for (i = 0; i <100; i++)
 * }                          |     b[i] = 2; 
 *                            |
 * 
 * (Example taken from Wikipedia's web page on loop Fission.)
 */
@SuppressWarnings("restriction")
public class LoopFissionRefactoring extends ForLoopRefactoring {

    private IASTForStatement loop = null;
    private IASTStatement body = null;

    public LoopFissionRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    protected void doCheckInitialConditions(RefactoringStatus initStatus) {
        // This gets the selected loop to re-factor and checks if the body is compound statement only..
        loop = getLoop();
        if (!(loop.getBody() instanceof IASTCompoundStatement)) {
            initStatus.addFatalError("The loop fission requires to have a compound body.");
        }
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        body = loop.getBody();
        ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
        IASTNode insert_parent = getLoop().getParent();
        IASTNode insert_before = getNextSibling(getLoop());
        IASTNode[] chilluns = body.getChildren();
        
        /*Over here, we are looking for all the statements in the body of for-loop
         *which can be put into individual for-loop of their own.
         */
        for (IASTNode child : chilluns) {
            IASTStatement stmt = (IASTStatement) (child.copy());
            IASTForStatement newForLoop = factory.newForStatement
                    (loop.getInitializerStatement().copy(), 
                            loop.getConditionExpression().copy(), 
                            loop.getIterationExpression().copy(), 
                            stmt);
            rewriter.insertBefore(insert_parent, insert_before, newForLoop, null);
        }
            /*This check is to make sure that compound body is not empty. 
             * If yes, it'll not perform re-factoring.
             */
            if (chilluns.length>0) {
                rewriter.remove(loop, null);
            }
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }

}
