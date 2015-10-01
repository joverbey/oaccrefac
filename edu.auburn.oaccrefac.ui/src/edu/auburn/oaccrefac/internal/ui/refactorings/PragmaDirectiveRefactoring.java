package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
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

import edu.auburn.oaccrefac.internal.core.ASTUtil;

@SuppressWarnings("restriction")
public class PragmaDirectiveRefactoring extends CRefactoring {

    private IASTTranslationUnit ast;
    private IASTPreprocessorPragmaStatement pragma;

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

//        if (statements.length == 0) {
//            initStatus.addFatalError("Selected region contains no statements on which to perform the refactoring.");
//            return initStatus;
//        }
//
//        String msg = String.format("Selected %d stmts (l%d - l%d)", statements.length,
//                statements[0].getFileLocation().getStartingLineNumber(),
//                statements[statements.length - 1].getFileLocation().getEndingLineNumber());
//        initStatus.addInfo(msg);

        pm.subTask("Checking initial conditions...");
        doCheckInitialConditions(initStatus, pm);
        return initStatus;
    }

    @Override
    protected RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext checkContext)
            throws CoreException, OperationCanceledException {
        RefactoringStatus result = new RefactoringStatus();
        pm.subTask("Determining if transformation can be safely performed...");
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

    @Override
    protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
            throws CoreException, OperationCanceledException {
        // TODO Auto-generated method stub
    }

    private IASTPreprocessorPragmaStatement findPragma() {
        IASTPreprocessorStatement[] pps = ast.getAllPreprocessorStatements();
        IASTPreprocessorPragmaStatement prag;
        for(IASTPreprocessorStatement pp : pps) {
            if(pp instanceof IASTPreprocessorPragmaStatement) {
                IASTPreprocessorPragmaStatement thisPrag = (IASTPreprocessorPragmaStatement) pp;
                int pragStart = thisPrag.getFileLocation().getNodeOffset();
                int pragLen = thisPrag.getFileLocation().getNodeLength();
            }
        }
        return null;
    }

    public IASTPreprocessorPragmaStatement getPragma() {
        return pragma;
    }

}
