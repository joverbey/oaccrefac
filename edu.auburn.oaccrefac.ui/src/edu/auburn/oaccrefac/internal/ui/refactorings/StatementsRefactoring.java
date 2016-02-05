package edu.auburn.oaccrefac.internal.ui.refactorings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTNode;
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

import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

@SuppressWarnings("restriction")
public abstract class StatementsRefactoring extends CRefactoring {

    private IASTTranslationUnit ast;
    private IASTStatement[] statements;
    private IASTNode[] statementsAndComments;

    public StatementsRefactoring(ICElement element, ISelection selection, ICProject project) {
        super(element, selection, project);
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
            throws CoreException, OperationCanceledException {
        if (initStatus.hasFatalError()) {
            return initStatus;
        }

        ast = getAST(tu, pm);
        discoverStatementsFromRegion();

        if (statements.length == 0) {
            initStatus.addFatalError("Selected region contains no statements on which to perform the refactoring.");
            return initStatus;
        }

        String msg = String.format("Selected %d stmts (l%d - l%d)", statements.length,
                statements[0].getFileLocation().getStartingLineNumber(),
                statements[statements.length - 1].getFileLocation().getEndingLineNumber());
        initStatus.addInfo(msg);

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

    protected abstract void refactor(IASTRewrite rewriter, IProgressMonitor pm) throws CoreException;

    @Override
    protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
            throws CoreException, OperationCanceledException {
        pm.subTask("Calculating modifications...");
        ASTRewrite rewriter = collector.rewriterForTranslationUnit(refactoringContext.getAST(getTranslationUnit(), pm));

        refactor(new CDTASTRewriteProxy(rewriter), pm);
    }

    private void discoverStatementsFromRegion() {
        List<IASTStatement> statements = new LinkedList<IASTStatement>();
        List<IASTComment> comments = new LinkedList<IASTComment>();
        List<IASTNode> statementsAndComments = new LinkedList<IASTNode>();

        List<IASTStatement> all = ASTUtil.find(ast, IASTStatement.class);
        int regionBegin = selectedRegion.getOffset();
        int regionEnd = selectedRegion.getLength() + regionBegin;

        // filter out statements not within the region bounds
        for (IASTStatement stmt : all) {
            int stmtBegin = stmt.getFileLocation().getNodeOffset();
            int stmtEnd = stmtBegin + stmt.getFileLocation().getNodeLength();
            if (stmtBegin >= regionBegin && stmtEnd <= regionEnd) {
                statements.add(stmt);
            }
        }

        // filter out statements that are children of other statements in the region
        Set<IASTStatement> children = new HashSet<IASTStatement>();
        for (IASTStatement child : statements) {
            for (IASTStatement parent : statements) {
                if (ASTUtil.isStrictAncestor(parent, child)) {
                    children.add(child);
                }
            }
        }
        for (Iterator<IASTStatement> iterator = statements.iterator(); iterator.hasNext();) {
            if (children.contains(iterator.next())) {
                iterator.remove();
            }
        }

        // filter out comments that are not within region bounds
        for (IASTComment comment : ast.getComments()) {
            int commentBegin = comment.getFileLocation().getNodeOffset();
            int commentEnd = commentBegin + comment.getFileLocation().getNodeLength();
            if (commentBegin >= regionBegin && commentEnd <= regionEnd) {
                comments.add(comment);
            }
        }

        // filter out comments that are inside of statements in the region
        Set<IASTComment> containedComments = new HashSet<IASTComment>();
        for (IASTComment com : comments) {
            for (IASTStatement stmt : statements) {
                if (ASTUtil.doesNodeLexicallyContain(stmt, com)) {
                    containedComments.add(com);
                }
            }
        }
        for (Iterator<IASTComment> iterator = comments.iterator(); iterator.hasNext();) {
            if (containedComments.contains(iterator.next())) {
                iterator.remove();
            }
        }
        statementsAndComments.addAll(statements);
        statementsAndComments.addAll(comments);
        Collections.sort(statements, ASTUtil.FORWARD_COMPARATOR);
        Collections.sort(statementsAndComments, ASTUtil.FORWARD_COMPARATOR);

        this.statements = statements.toArray(new IASTStatement[statements.size()]);
        this.statementsAndComments = statementsAndComments.toArray(new IASTNode[statementsAndComments.size()]);

    }

    public IASTStatement[] getStatements() {
        return statements;
    }

    public IASTNode[] getStatementsAndComments() {
        return statementsAndComments;
    }
}
