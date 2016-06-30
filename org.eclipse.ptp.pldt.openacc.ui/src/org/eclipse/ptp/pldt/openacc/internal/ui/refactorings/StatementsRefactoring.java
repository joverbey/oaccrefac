package org.eclipse.ptp.pldt.openacc.internal.ui.refactorings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
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
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

@SuppressWarnings("restriction")
public abstract class StatementsRefactoring extends CRefactoring {

    private IASTTranslationUnit ast;
    private IASTStatement[] statements;
    private IASTNode[] allEnclosedNodes;

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
            initStatus.addFatalError(Messages.StatementsRefactoring_NoStatementsToRefactor);
            return initStatus;
        }

        /*
        String msg = String.format("Selected %d stmts (l%d - l%d)", statements.length,
                statements[0].getFileLocation().getStartingLineNumber(),
                statements[statements.length - 1].getFileLocation().getEndingLineNumber());
        initStatus.addInfo(msg);
        */

        pm.subTask(Messages.StatementsRefactoring_CheckingInitialConditions);
        doCheckInitialConditions(initStatus, pm);
        return initStatus;
    }
    
    @Override
    protected IASTTranslationUnit getAST(ITranslationUnit tu, IProgressMonitor pm) throws OperationCanceledException, CoreException {
        IASTTranslationUnit ast = null;
    	// HACK to avoid some concurrency issue that only appears when using the JUnit test runner
        boolean failed = false;
        int attempts = 0;
        do {
        	try {
        		ast = super.getAST(tu, pm);
        		failed = false;
        	} catch (NullPointerException e) {
        		failed = true;
        		attempts++;
        		try {
					Thread.sleep(10);
				} catch (InterruptedException e1) {
				}
        	}
        } while (failed && attempts < 5);
        return ast;
    }

    @Override
    protected RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext checkContext)
            throws CoreException, OperationCanceledException {
        RefactoringStatus result = new RefactoringStatus();
        pm.subTask(Messages.StatementsRefactoring_DeterminingIfSafe);
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
        pm.subTask(Messages.StatementsRefactoring_CalculatingModifications);
        ASTRewrite rewriter = collector.rewriterForTranslationUnit(refactoringContext.getAST(getTranslationUnit(), pm));

        refactor(new CDTASTRewriteProxy(rewriter), pm);
    }

    private void discoverStatementsFromRegion() {
        int regionBegin = selectedRegion.getOffset();
        int regionEnd = selectedRegion.getLength() + regionBegin;

        List<IASTStatement> statements = collectStatements(regionBegin, regionEnd);
        List<IASTComment> comments = collectComments(regionBegin, regionEnd, statements);
        List<IASTPreprocessorStatement> preprocs = collectPreprocessorStatements(regionBegin, regionEnd, statements);
        List<IASTNode> allEnclosedNodes = new LinkedList<IASTNode>();
        
        allEnclosedNodes.addAll(statements);
        allEnclosedNodes.addAll(comments);
        allEnclosedNodes.addAll(preprocs);
        
        Collections.sort(statements, ASTUtil.FORWARD_COMPARATOR);
        Collections.sort(allEnclosedNodes, ASTUtil.FORWARD_COMPARATOR);

        this.statements = statements.toArray(new IASTStatement[statements.size()]);
        this.allEnclosedNodes = allEnclosedNodes.toArray(new IASTNode[allEnclosedNodes.size()]);

    }
    
    private List<IASTStatement> collectStatements(int regionBegin, int regionEnd) {
        List<IASTStatement> all = ASTUtil.find(ast, IASTStatement.class);
        List<IASTStatement> statements = new LinkedList<>();
        //filter out statements not within the region bounds
        for (IASTStatement stmt : all) {
            int stmtBegin = stmt.getFileLocation().getNodeOffset();
            int stmtEnd = stmtBegin + stmt.getFileLocation().getNodeLength();
            if (stmtBegin >= regionBegin && stmtEnd <= regionEnd) {
                statements.add(stmt);
            }
        }

        //filter out statements that are children of other statements in the region
        Set<IASTStatement> children = new HashSet<IASTStatement>();
        for (IASTStatement child : statements) {
            for (IASTStatement parent : statements) {
                if (ASTUtil.isStrictAncestor(child, parent)) {
                    children.add(child);
                }
            }
        }
        for (Iterator<IASTStatement> iterator = statements.iterator(); iterator.hasNext();) {
            if (children.contains(iterator.next())) {
                iterator.remove();
            }
        }
        
        return statements;
    }
    
    private List<IASTComment> collectComments(int regionBegin, int regionEnd, List<IASTStatement> statements) {
        List<IASTComment> comments = new LinkedList<IASTComment>();
        
        //filter out comments that are not within region bounds
        for (IASTComment comment : ast.getComments()) {
            int commentBegin = comment.getFileLocation().getNodeOffset();
            int commentEnd = commentBegin + comment.getFileLocation().getNodeLength();
            if (commentBegin >= regionBegin && commentEnd <= regionEnd) {
                comments.add(comment);
            }
        }

        //filter out comments that are inside of statements in the region
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
        
        return comments;
    }

    private List<IASTPreprocessorStatement> collectPreprocessorStatements(int regionBegin, int regionEnd, List<IASTStatement> statements) {
        List<IASTPreprocessorStatement> preprocs = new LinkedList<IASTPreprocessorStatement>();
        
        //filter out comments that are not within region bounds
        for (IASTPreprocessorStatement preproc : ast.getAllPreprocessorStatements()) {
            int preprocBegin = preproc.getFileLocation().getNodeOffset();
            int preprocEnd = preprocBegin + preproc.getFileLocation().getNodeLength();
            if (preprocBegin >= regionBegin && preprocEnd <= regionEnd) {
                preprocs.add(preproc);
            }
        }

        //filter out comments that are inside of statements in the region
        Set<IASTPreprocessorStatement> containedPreprocs = new HashSet<IASTPreprocessorStatement>();
        for (IASTPreprocessorStatement com : preprocs) {
            for (IASTStatement stmt : statements) {
                if (ASTUtil.doesNodeLexicallyContain(stmt, com)) {
                    containedPreprocs.add(com);
                }
            }
        }
        for (Iterator<IASTPreprocessorStatement> iterator = preprocs.iterator(); iterator.hasNext();) {
            if (containedPreprocs.contains(iterator.next())) {
                iterator.remove();
            }
        }
        
        return preprocs;
    }
    
    public IASTStatement[] getStatements() {
        return statements;
    }

    public IASTNode[] getAllEnclosedNodes() {
        return allEnclosedNodes;
    }
}
