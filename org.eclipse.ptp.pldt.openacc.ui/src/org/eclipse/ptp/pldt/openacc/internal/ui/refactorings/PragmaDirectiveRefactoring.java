package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;

@SuppressWarnings("restriction")
public abstract class PragmaDirectiveRefactoring extends CRefactoring {

    private IASTTranslationUnit ast;
    private IASTPreprocessorPragmaStatement pragma;
    private IASTStatement statement;

    public PragmaDirectiveRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        if (initStatus.hasFatalError()) {
            return initStatus;
        }

        ast = getAST(tu, pm);

        pragma = findPragma();
        if (pragma == null) {
            initStatus.addFatalError(Messages.PragmaDirectiveRefactoring_SelectAPragma);
            return initStatus;
        }

        statement = findStatement(pragma);

        String msg = NLS.bind(Messages.PragmaDirectiveRefactoring_SelectedPragmaInfo, 
        		new Object[] { pragma.getRawSignature(), pragma.getFileLocation().getStartingLineNumber() });
        initStatus.addInfo(msg);

        pm.subTask(Messages.Refactoring_CheckingInitialConditions);
        doCheckInitialConditions(initStatus, pm);
        return initStatus;
    }

    @Override
    protected RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext checkContext)
            throws CoreException, OperationCanceledException {
        RefactoringStatus result = new RefactoringStatus();
        pm.subTask(Messages.Refactoring_DeterminingIfSafe);
        doCheckFinalConditions(result, pm);
        return result;
    }

    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) {
    }

    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) {
    }

    @Override
    protected RefactoringDescriptor getRefactoringDescriptor() {
        return null;
    }

    protected abstract void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException;

    @Override
    protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
            throws CoreException, OperationCanceledException {
        pm.subTask(Messages.Refactoring_CalculatingModifications);
        ASTRewrite rewriter = collector.rewriterForTranslationUnit(refactoringContext.getAST(getTranslationUnit(), pm));

        refactor(new CDTASTRewriteProxy(rewriter), pm);
    }

    private IASTPreprocessorPragmaStatement findPragma() {
        IASTPreprocessorStatement[] pps = ast.getAllPreprocessorStatements();
        List<IASTPreprocessorPragmaStatement> selectedPrags = new ArrayList<IASTPreprocessorPragmaStatement>();
        for (IASTPreprocessorStatement pp : pps) {
            if (pp instanceof IASTPreprocessorPragmaStatement) {
                IASTPreprocessorPragmaStatement thisPrag = (IASTPreprocessorPragmaStatement) pp;
                int pragStart = thisPrag.getFileLocation().getNodeOffset();
                int pragEnd = thisPrag.getFileLocation().getNodeLength() + pragStart;
                int selStart = selectedRegion.getOffset();
                int selEnd = selectedRegion.getLength() + selStart;
                if (pragEnd > selStart && pragStart < selEnd) {
                    selectedPrags.add(thisPrag);
                }
            }
        }
        return selectedPrags.size() > 0 ? selectedPrags.get(0) : null;
    }

    private IASTStatement findStatement(IASTPreprocessorPragmaStatement pragma) {

        class PragmaStatementFinder extends ASTVisitor {

            IASTStatement nearestFollowingStatement;
            int after;

            public PragmaStatementFinder(IASTPreprocessorPragmaStatement pragma) {
                shouldVisitStatements = true;
                after = pragma.getFileLocation().getNodeOffset() + pragma.getFileLocation().getNodeLength();
            }

            @Override
            public int visit(IASTStatement stmt) {
                if (stmt.getFileLocation().getNodeOffset() >= after) {
                    if(nearestFollowingStatement == null || stmt.getFileLocation().getNodeOffset() < nearestFollowingStatement.getFileLocation().getNodeOffset()) {
                        nearestFollowingStatement = stmt;
                                }
                }
                return PROCESS_CONTINUE;
            }

        }

        PragmaStatementFinder finder = new PragmaStatementFinder(pragma);
        ast.accept(finder);
        return finder.nearestFollowingStatement;

    }

    public IASTPreprocessorPragmaStatement getPragma() {
        return pragma;
    }

    public IASTStatement getStatement() {
        return statement;
    }

}
