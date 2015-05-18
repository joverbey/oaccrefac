package edu.auburn.oaccrefac.internal.core;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;

public class ASTUtil {
    public static <T> List<T> find(IASTNode parent, Class<T> clazz) {
        List<T> results = new LinkedList<T>();
        findAndAdd(parent, clazz, results);
        return results;
    }

    public static <T> T findOne(IASTNode parent, Class<T> clazz) {
        List<T> results = find(parent, clazz);
        if (results.size() == 0) {
            throw new RuntimeException("Failed to find any " + clazz.getName() + " in " + parent.toString());
        }

        return results.get(0);
    }

    public static IASTTranslationUnit translationUnitForFile(String file) throws CoreException {
        IParserLogService log = new DefaultLogService();
        FileContent fileContent = FileContent.createForExternalFileLocation(file);

        Map<String, String> definedSymbols = new HashMap<String, String>();
        String[] includePaths = new String[0];
        IScannerInfo scanInfo = new ScannerInfo(definedSymbols, includePaths);
        IncludeFileContentProvider fileContentProvider = IncludeFileContentProvider.getEmptyFilesProvider();
        IASTTranslationUnit translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, scanInfo,
                fileContentProvider, null, 0, log);
        return translationUnit;
    }

    public static void printRecursive(IASTNode node, int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }

        System.out.println("[" + node.getClass().getName() + "] " + node);

        for (IASTNode child : node.getChildren()) {
            printRecursive(child, indent + 2);
        }
    }

    private static <T> void findAndAdd(IASTNode parent, Class<T> clazz, List<T> results) {
        if (clazz.isInstance(parent)) {
            results.add(clazz.cast(parent));
        }

        for (IASTNode child : parent.getChildren()) {
            findAndAdd(child, clazz, results);
        }
    }

    /**
     * Raises an exception with line number information
     * 
     * @param message
     *            the exception message
     * @param node
     *            the IASTNode to extract the line number information from
     * @throws RuntimeException
     */
    static void raise(String message, IASTNode node) {
        throw new RuntimeException(message + " at line " + node.getFileLocation().getStartingLineNumber());
    }
}
