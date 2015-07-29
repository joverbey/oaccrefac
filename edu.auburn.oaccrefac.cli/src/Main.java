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
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import edu.auburn.oaccrefac.cli.dom.rewrite.ASTRewrite;
import edu.auburn.oaccrefac.core.change.IASTRewrite;
import edu.auburn.oaccrefac.core.change.UnrollLoop;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: refactor <filename.c>");
            System.exit(1);
        }

        String filename = args[0];
        IASTTranslationUnit translationUnit = null;
        try {
            translationUnit = parse(filename);
        } catch (CoreException e) {
            System.err.printf("Unable to parse %s (%s)\n", filename, e.getMessage());
            System.exit(1);
        }

        IASTRewrite rw = ASTRewrite.create(translationUnit);
        IASTForStatement forLoop = ASTUtil.findOne(translationUnit, IASTForStatement.class);
        // rw.replace(forLoop, rw.createLiteralNode("/* For loop is gone */"), new TextEditGroup("Remove loop"));

        UnrollLoop unroll = new UnrollLoop(rw, forLoop, 4);
        RefactoringStatus status = unroll.checkConditions(new RefactoringStatus(), null);
        printStatus(status);

        if (status.hasFatalError()) {
            System.exit(1);
        }

        Change chg = unroll.change().rewriteAST();
        try {
            chg.perform(new NullProgressMonitor());
        } catch (CoreException e) {
            System.err.printf("Internal error creating change: %s\n", e.getMessage());
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