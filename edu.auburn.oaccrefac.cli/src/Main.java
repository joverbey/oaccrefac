/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import edu.auburn.oaccrefac.cli.dom.rewrite.ASTRewrite;
import edu.auburn.oaccrefac.cli.dom.rewrite.ASTRewrite.CommentPosition;
import edu.auburn.oaccrefac.core.transformations.Check;
import edu.auburn.oaccrefac.core.transformations.IASTRewrite;
import edu.auburn.oaccrefac.core.transformations.RefactoringParams;
import edu.auburn.oaccrefac.core.transformations.SourceAlteration;

/**
 * Skeleton command line driver.  Subclasses should extend to implement specific refactorings.
 */
public abstract class Main<P extends RefactoringParams, C extends Check<P>, A extends SourceAlteration<C>> {

    protected final void run(String[] args) {
        if (!checkArgs(args)) {
            System.exit(1);
        }

        String filename = args[0];
        IASTTranslationUnit translationUnit = null;
        try {
            translationUnit = parse(filename);
        } catch (CoreException e) {
            System.err.printf("Unable to parse %s (%s)\n", filename, e.getMessage());
            System.exit(2);
        }

        IASTRewrite rw = ASTRewrite.create(translationUnit);

        IASTForStatement forLoop = findLoopToAutotune(translationUnit, rw);
        if (forLoop == null) {
            System.err.println(
                    "Please add a comment containing \"autotune\" or \"refactor\" immediately above the loop to refactor.");
            System.exit(3);
        }

        C check = createCheck(forLoop);
        RefactoringStatus status = check.performChecks(new RefactoringStatus(), new NullProgressMonitor(), createParams(forLoop));
        printStatus(status);
        if (status.hasFatalError()) {
            System.exit(4);
        }

        try {
            A xform = createAlteration(rw, check);
            xform.change();
            xform.rewriteAST().perform(new NullProgressMonitor());
        } catch (CoreException e) {
            System.err.printf("Internal error creating change: %s\n", e.getMessage());
            System.exit(5);
        }
    }

    private static IASTForStatement findLoopToAutotune(IASTTranslationUnit translationUnit, final IASTRewrite rw) {
        class V extends ASTVisitor {
            private IASTForStatement found = null;

            public V() {
                this.shouldVisitStatements = true;
            }

            @Override
            public int visit(IASTStatement statement) {
                if (found == null && statement instanceof IASTForStatement) {
                    for (IASTComment comment : ((ASTRewrite) rw).getComments(statement, CommentPosition.leading)) {
                        String commentLower = String.valueOf(comment.getComment()).toLowerCase();
                        if (commentLower.contains("autotune") || commentLower.contains("refactor")) {
                            found = (IASTForStatement) statement;
                            return PROCESS_ABORT;
                        }

                    }
                }
                return PROCESS_CONTINUE;
            }
        }
        V visitor = new V();
        translationUnit.accept(visitor);
        return visitor.found;
    }

    private static void printStatus(RefactoringStatus status) {
        for (RefactoringStatusEntry entry : status.getEntries()) {
            System.err.println(entry);
        }
    }

    private static IASTTranslationUnit parse(String filename) throws CoreException {
        IParserLogService log = new DefaultLogService();
        FileContent fileContent = FileContent.createForExternalFileLocation(filename);

        Map<String, String> definedSymbols = new HashMap<String, String>();
        String[] includePaths = new String[0];
        IScannerInfo scanInfo = new ScannerInfo(definedSymbols, includePaths);
        IncludeFileContentProvider fileContentProvider = IncludeFileContentProvider.getEmptyFilesProvider();
        IASTTranslationUnit translationUnit = GCCLanguage.getDefault().getASTTranslationUnit(fileContent, scanInfo,
                fileContentProvider, null, 0, log);
        return translationUnit;
    }

    protected abstract boolean checkArgs(String[] args);

    protected abstract C createCheck(IASTForStatement loop);

    protected abstract P createParams(IASTForStatement forLoop);

    protected abstract A createAlteration(IASTRewrite rewriter, C check) throws CoreException;
}