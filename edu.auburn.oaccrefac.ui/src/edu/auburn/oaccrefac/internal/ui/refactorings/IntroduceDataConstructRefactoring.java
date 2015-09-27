package edu.auburn.oaccrefac.internal.ui.refactorings;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

@SuppressWarnings("restriction")
public class IntroduceDataConstructRefactoring extends CRefactoring {

    private IASTTranslationUnit ast;
    
    public IntroduceDataConstructRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
        
        if (selection == null || tu.getResource() == null || project == null)
            initStatus.addFatalError("Invalid selection");
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }
    
    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        ast = getAST(getTranslationUnit(), pm);
        //TODO create check here
        return initStatus;
    }

    @Override
    protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
            throws CoreException, OperationCanceledException {
        
        pm.subTask("Calculating modifications...");
        ASTRewrite rewriter = collector.rewriterForTranslationUnit(refactoringContext.getAST(getTranslationUnit(), pm));

        refactor(new CDTASTRewriteProxy(rewriter), pm);

    }

    private void refactor(CDTASTRewriteProxy cdtastRewriteProxy, IProgressMonitor pm) {
        //TODO create alteration here
    }

}
