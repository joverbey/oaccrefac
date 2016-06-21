package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
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
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;

@SuppressWarnings("restriction")
public abstract class SourceFileRefactoring extends CRefactoring {

	private IASTTranslationUnit ast;
	private Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> pragmas;

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

		pragmas = findPragmas();
		if (pragmas.size() == 0) {
			initStatus.addFatalError("Selected File does not contain any pragma");
			return initStatus;
		}
		List<IASTStatement> statements = new ArrayList<IASTStatement>();
		for (IASTPreprocessorPragmaStatement p : pragmas.keySet()) {
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

	private Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> findPragmas() {
		Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> pragmasInFile = new HashMap<IASTPreprocessorPragmaStatement, List<IASTForStatement>>();
		IASTPreprocessorStatement[] pps = ast.getAllPreprocessorStatements();
		for (int i = 0; i < pps.length; i++) {
			if (pps[i] instanceof IASTPreprocessorPragmaStatement) {
				IASTPreprocessorPragmaStatement pragma = (IASTPreprocessorPragmaStatement) pps[i];
				IASTStatement enclosingStmt = findStatementWithPragma(pragma);
				if (enclosingStmt instanceof IASTForStatement) {
					if (!pragma.getRawSignature().contains("loop")) {
						pragmasInFile.put(pragma, null);
					}
					else {
						IASTForStatement outerLoop = (IASTForStatement) enclosingStmt;
						ForStatementInquisitor inq = ForStatementInquisitor.getInquisitor(outerLoop);
						List<IASTForStatement> loopHeaders = inq.getLoopHeaders();
						pragmasInFile.put(pragma, loopHeaders);
						if (loopHeaders.size() > 1) {
							for (int j = 1; j < loopHeaders.size(); j++) {
								inq = ForStatementInquisitor.getInquisitor(loopHeaders.get(j));
								if (inq.getLeadingPragmas().size() > 0) {
									i += inq.getLeadingPragmas().size();
								}
							}
						}
					}
					
				} else {
					pragmasInFile.put(pragma, null);
				}
			}
		}
		return pragmasInFile;
	}

	private IASTStatement findStatementWithPragma(IASTPreprocessorPragmaStatement pragma) {
		class StatementFinder extends ASTVisitor {
			IASTStatement nearestFollowingStatement;
			int after;

			public StatementFinder(IASTPreprocessorPragmaStatement pragma) {
				shouldVisitStatements = true;
				after = pragma.getFileLocation().getNodeOffset() + pragma.getFileLocation().getNodeLength();
			}

			@Override
			public int visit(IASTStatement stmt) {

				if (stmt.getFileLocation().getNodeOffset() >= after) {
					if (nearestFollowingStatement == null || stmt.getFileLocation().getNodeOffset() < nearestFollowingStatement
							.getFileLocation().getNodeOffset()) {
						nearestFollowingStatement = stmt;
					}
				}

				return PROCESS_CONTINUE;
			}
		}
		StatementFinder finder = new StatementFinder(pragma);
		ast.accept(finder);
		return finder.nearestFollowingStatement;
	}

	public Map<IASTPreprocessorPragmaStatement, List<IASTForStatement>> getPragmas() {
		return pragmas;
	}
}
