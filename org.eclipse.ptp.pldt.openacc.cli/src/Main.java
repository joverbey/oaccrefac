
/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
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
import org.eclipse.ptp.pldt.openacc.core.transformations.Check;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.core.transformations.RefactoringParams;
import org.eclipse.ptp.pldt.openacc.core.transformations.SourceAlteration;

import edu.auburn.oaccrefac.cli.dom.rewrite.ASTRewrite;

/**
 * Main serves as a generic base for any refactoring Runnables.
 *
 * @param <S>
 *            Statement type refactoring uses.
 * @param <P>
 *            Refactoring parameters.
 * @param <C>
 *            Checker.
 * @param <A>
 *            Source alteration.
 */
public abstract class Main<S extends IASTStatement, P extends RefactoringParams, C extends Check<P>, A extends SourceAlteration<C>>
        implements Runnable {

    /**
     * run satisfies the Runnable interface so that refactorings can be generically run.
     * 
     * @param args
     *            Arguments passed to the refactoring.
     */
    public final void run(String[] args) {
        boolean expectLoopName = false;
        String loopName = new String();
        if (args.length > 1) {
            if (args[1].equals("-ln")) {
                expectLoopName = true;
                loopName = args[2];
            }
        }
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
        S statement = findStatementToAutotune(translationUnit, rw, loopName, expectLoopName);
        if (statement == null) {
            System.err.println(
                    "Please add a comment containing the loop name entered immediately above the loop to refactor.");
            System.exit(3);
        }
        C check = createCheck(statement);
        RefactoringStatus status = check.performChecks(new RefactoringStatus(), new NullProgressMonitor(),
                createParams(statement));
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

    /**
     * printStatus prints all entries in a refactoring status.
     * 
     * @param status
     *            Status to print.
     */
    private void printStatus(RefactoringStatus status) {
        for (RefactoringStatusEntry entry : status.getEntries()) {
            System.err.println(entry);
        }
    }

    /**
     * Parses a file for the translation unit which represents it.
     * 
     * @param filename
     *            Name of file to parse.
     * @return Translation unit of file.
     * @throws CoreException
     *             If getting the translation unit fails.
     */
    private IASTTranslationUnit parse(String filename) throws CoreException {
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

    /**
     * findStatementToAutotune should returns a statement of type S which will be passed to the alteration.
     * 
     * @param translationUnit
     *            Translation unit being altered
     * @param rw
     *            Rewriter for the translation unit.
     * @param loopName
     *            Name of statement to be altered
     * @return Statement to be altered.
     */
    protected S findStatementToAutotune(final IASTTranslationUnit translationUnit, final IASTRewrite rw,
            final String loopName, final boolean expectLoopName) {
        class V extends ASTVisitor {
            private S found = null;

            ArrayList<Integer> pragmaPositions;

            public V() {
                this.shouldVisitStatements = true;
                pragmaPositions = new ArrayList<>();
                for (IASTPreprocessorStatement statement : translationUnit.getAllPreprocessorStatements()) {
                    pragmaPositions.add(statement.getFileLocation().getStartingLineNumber());
                }
                Collections.sort(pragmaPositions);
            }

            @Override
            public int visit(IASTStatement statement) {
                if (found == null) {
                    int pragmasToSkip = 0;
                    int current = Collections.binarySearch(pragmaPositions,
                            statement.getFileLocation().getStartingLineNumber() - 1);
                    if (current >= 0) {
                        pragmasToSkip++;
                        while (current > 0 && pragmaPositions.get(current - 1) == pragmaPositions.get(current) - 1) {
                            current--;
                            pragmasToSkip++;
                        }
                    }
                    for (IASTComment comment : translationUnit.getComments()) {
                        int start = comment.getFileLocation().getStartingLineNumber();
                        int finish = comment.getFileLocation().getEndingLineNumber();
                        if (start - finish == 0
                                && start == statement.getFileLocation().getStartingLineNumber() - (pragmasToSkip + 1)) {
                            String commentLower = String.valueOf(comment.getComment()).toLowerCase();
                            if (commentLower.contains(loopName) && expectLoopName) {
                                found = convertStatement(statement);
                                return PROCESS_ABORT;
                            }
                            if (!expectLoopName) {
                                if(commentLower.contains("autotune") || commentLower.contains("refactor")){
                                    found = convertStatement(statement);
                                }
                                return PROCESS_ABORT;
                            }
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

    /**
     * convertStatement should accept a statement and convert it to the type of S.
     * 
     * @param statement
     *            Statement to convert.loopName = args[2];
     * @return Converted statement.
     */
    protected abstract S convertStatement(IASTStatement statement);

    /**
     * checkArgs should check the arguments passed into an alteration.
     * 
     * @param args
     *            Arguments passed into an alteration.
     * @return Value representing whether or not the arguments passed the check.
     */
    protected abstract boolean checkArgs(String[] args);

    /**
     * createCheck creates the checker for an alteration.
     * 
     * @param statement
     *            Statement to check.
     * @return Checker.
     */
    protected abstract C createCheck(S statement);

    /**
     * createParams creates parameters for an alteration.
     * 
     * @param statement
     *            Statement to create the parameters for.
     * @return Params.
     */
    protected abstract P createParams(S statement);

    /**
     * createAlteration writes the alteration.
     * 
     * @param rewriter
     * @param check
     * @return Alteration to use on file.
     * @throws CoreException
     */
    protected abstract A createAlteration(IASTRewrite rewriter, C check) throws CoreException;
}