package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTForStatement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.auburn.oaccrefac.internal.core.ForLoopUtil;

/**
 * This class defines the implementation for re-factoring using loop fusion. For example:
 * 
 * ORIGINAL:                  REFACTORED: 
 * int i, a[100], b[100];    | int i, a[100], b[100]; 
 * for (i = 0; i < 100; i++) | for (i = 0; i <100; i++)
 *    a[i] = 1;              | { 
 * for (i = 0; i < 100; i++) | a[i] = 1; 
 *    b[i] = 2;              | b[i] = 2; 
 *                           | }
 * 
 * (Example taken from Wikipedia's web page on loop Fusion)
 */
@SuppressWarnings("restriction")
public class LoopFusionRefactoring extends ForLoopRefactoring {

    private IASTForStatement nxtloop = null;
    private IASTForStatement loop = null;
    private IASTStatement body = null;
    private IASTCompoundStatement body_compound = null;
    private IASTCompoundStatement bdy_compound = null;
    private IASTCompoundStatement newbody = null;

    public LoopFusionRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus initStatus, IProgressMonitor pm) {
        // This gets the selected loop to re-factor.
        loop = getLoop();
        boolean found = false;
        IASTNode newnode = loop;
        while (ForLoopUtil.getNextSibling(newnode) != null && !found) {
            newnode = ForLoopUtil.getNextSibling(newnode);
            nxtloop = findLoop(newnode);
            found = (nxtloop != null);
        }

        if (!found) {
            initStatus.addFatalError("There is no for loop for fusion to be possible.");
        }
    }
    private IASTCompoundStatement convertIntoCompound(IASTStatement bdy)
    {
        // if the body is not within braces, i.e. short-cut
        // loop pattern, make it a compounded statement
        if (!(bdy instanceof IASTCompoundStatement)) {
            ICNodeFactory factory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
            bdy_compound = factory.newCompoundStatement();
            bdy_compound.addStatement(bdy.copy());
        } else {
            bdy_compound = (IASTCompoundStatement) (bdy.copy());
            }
        return bdy_compound;
    }

    @Override
    protected void refactor(ASTRewrite rewriter, IProgressMonitor pm) {
        body = getLoop().getBody();
        body_compound = convertIntoCompound(body);
        IASTStatement loopbody = nxtloop.getBody();
        newbody = convertIntoCompound(loopbody);
        addbody((IASTCompoundStatement) body_compound, newbody);
        CASTForStatement newFor = getLoop().copy();
        newFor.setBody(body_compound);
        rewriter.replace(getLoop(), newFor, null);
        rewriter.remove(nxtloop, null);
    }

    private void addbody(IASTCompoundStatement body_compound, IASTCompoundStatement loopbody) {
        for (IASTNode childz : loopbody.getChildren()) {
            body_compound.addStatement((IASTStatement) childz.copy());
        }

    }

    private IASTForStatement findLoop(IASTNode tree) {
        class LoopFinder extends ASTVisitor {
            private IASTForStatement forloop = null;

            public LoopFinder() {
                shouldVisitStatements = true;
            }

            @Override
            public int visit(IASTStatement visitor) {
                if (visitor instanceof IASTForStatement) {
                    forloop = (IASTForStatement) visitor;
                    return ASTVisitor.PROCESS_ABORT;
                } else {
                    return ASTVisitor.PROCESS_CONTINUE;
                }
            }
        }

        LoopFinder finder = new LoopFinder();
        tree.accept(finder);
        return finder.forloop;
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }

}
