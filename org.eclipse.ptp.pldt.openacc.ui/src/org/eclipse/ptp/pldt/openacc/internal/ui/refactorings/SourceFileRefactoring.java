package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import java.util.ArrayList;
import java.util.LinkedList;
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
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;


@SuppressWarnings("restriction")
public abstract class SourceFileRefactoring extends CRefactoring{

    private IASTTranslationUnit ast;
    private List<IASTPreprocessorPragmaStatement> pragma;
    private List<IASTStatement> statements;

    public SourceFileRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);

        if (selection == null || tu.getResource() == null || project == null) {
            initStatus.addFatalError("Invalid selection");
        }
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        if (initStatus.hasFatalError()) {
            return initStatus;
        }

        ast = getAST(tu, pm);

        pragma = findPragma();
        if (pragma.size() == 0) {
            initStatus.addFatalError("Selected File does not contain any OpenACC pragma");
            return initStatus;
        }
        statements = new ArrayList<IASTStatement>();
        for (IASTPreprocessorPragmaStatement p : pragma) {
            IASTStatement stmt = findStatementWithPragma(p);
            statements.add(stmt);
            String msg = String.format("Selected \"%s\" on line %d", p.getRawSignature(),
                    p.getFileLocation().getStartingLineNumber());
            initStatus.addInfo(msg);

            pm.subTask("Checking initial conditions...");
            doCheckInitialConditions(initStatus, pm);
        }
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

    protected abstract void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException;

    @Override
    protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
            throws CoreException, OperationCanceledException {
        pm.subTask("Calculating modifications...");
        ASTRewrite rewriter = collector.rewriterForTranslationUnit(refactoringContext.getAST(getTranslationUnit(), pm));

        refactor(new CDTASTRewriteProxy(rewriter), pm);
    }

    private List<IASTPreprocessorPragmaStatement> findPragma() {
        IASTPreprocessorStatement[] pps = ast.getAllPreprocessorStatements();
        List<IASTPreprocessorPragmaStatement> pragmasInFile = new LinkedList<IASTPreprocessorPragmaStatement>();
        for (IASTPreprocessorStatement pp : pps) {
            if (pp instanceof IASTPreprocessorPragmaStatement) {
                IASTPreprocessorPragmaStatement pragma = (IASTPreprocessorPragmaStatement) pp;
                pragmasInFile.add(pragma);
            }
        }
        return pragmasInFile;
    }

    /*private IASTForStatement findForLoopWithPragma(IASTPreprocessorPragmaStatement pragma) {
       class ForLoopFinder extends ASTVisitor {
           IASTForStatement nearestFollowingStatement;
           int after;

           public ForLoopFinder(IASTPreprocessorPragmaStatement pragma) {
               shouldVisitStatements = true;
               after = pragma.getFileLocation().getNodeOffset() + pragma.getFileLocation().getNodeLength();
           }

           @Override
           public int visit(IASTStatement stmt) {
               if (stmt instanceof IASTForStatement) {
                   IASTForStatement statement = (IASTForStatement) stmt;
                   if (statement.getFileLocation().getNodeOffset() >= after) {
                       if(nearestFollowingStatement == null || statement.getFileLocation().getNodeOffset() < nearestFollowingStatement.getFileLocation().getNodeOffset()) {
                           nearestFollowingStatement = statement;
                                   }
                   }
               }

               return PROCESS_CONTINUE;
           }
       }
       ForLoopFinder finder = new ForLoopFinder(pragma);
       ast.accept(finder);
       return finder.nearestFollowingStatement;
    }*/

    private IASTStatement findStatementWithPragma(IASTPreprocessorPragmaStatement pragma) {
        class ForLoopFinder extends ASTVisitor {
            IASTStatement nearestFollowingStatement;
            int after;

            public ForLoopFinder(IASTPreprocessorPragmaStatement pragma) {
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
        ForLoopFinder finder = new ForLoopFinder(pragma);
        ast.accept(finder);
        return finder.nearestFollowingStatement;
    }

    public List<IASTPreprocessorPragmaStatement> getPragma() {
        return pragma;
    }

    public List<IASTStatement> getStatement() {
        return statements;
    }
}
