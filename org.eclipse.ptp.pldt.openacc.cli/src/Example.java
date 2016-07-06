/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
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

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
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
import org.eclipse.ptp.pldt.openacc.core.transformations.DistributeLoopsAlteration;
import org.eclipse.ptp.pldt.openacc.core.transformations.DistributeLoopsCheck;
import org.eclipse.ptp.pldt.openacc.core.transformations.IASTRewrite;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

import edu.auburn.oaccrefac.cli.dom.rewrite.ASTRewrite;

// File might not need to exist any more.

/**
 * Example command line driver -- how to run a refactoring from the CLI.
 */
public class Example {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: example <filename.c>"); //$NON-NLS-1$
            System.exit(1);
        }

        String filename = args[0];
        IASTTranslationUnit translationUnit = null;
        try {
            translationUnit = parse(filename);
        } catch (CoreException e) {
            System.err.printf("Unable to parse %s (%s)\n", filename, e.getMessage()); //$NON-NLS-1$
            System.exit(1);
        }

        IASTRewrite rw = ASTRewrite.create(translationUnit);
        IASTForStatement forLoop = ASTUtil.findFirst(translationUnit, IASTForStatement.class);
        if (forLoop == null) {
            System.err.println("No loop found"); //$NON-NLS-1$
            System.exit(1);
        }

        // rw.replace(forLoop, rw.createLiteralNode("/* For loop is gone */"), new TextEditGroup("Remove loop"));

        DistributeLoopsCheck check = new DistributeLoopsCheck(new RefactoringStatus(), forLoop);
        RefactoringStatus status = check.performChecks(new NullProgressMonitor(), null);
        printStatus(status);
        if (status.hasFatalError()) {
            System.exit(1);
        }

        DistributeLoopsAlteration xform = new DistributeLoopsAlteration(rw, check);
        try {
            xform.change();
            xform.rewriteAST().perform(new NullProgressMonitor());
        } catch (CoreException e) {
            System.err.printf("Internal error creating change: %s\n", e.getMessage()); //$NON-NLS-1$
            System.exit(1);
        }
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
}