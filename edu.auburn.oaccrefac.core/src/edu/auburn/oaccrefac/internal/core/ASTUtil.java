package edu.auburn.oaccrefac.internal.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
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
import org.junit.Assert;

import edu.auburn.oaccrefac.core.newtmp.LinearExpression;

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

    @SuppressWarnings("unchecked")
    public static <T> T findNearestAncestor(IASTNode startingNode, Class<T> clazz) {
        for (IASTNode node = startingNode.getParent(); node != null; node = node.getParent()) {
            if (clazz.isInstance(node)) {
                return (T) node;
            }
        }
        return null;
    }

    public static IASTTranslationUnit translationUnitForFile(String file) throws CoreException {
        return translationUnitForFileContent(FileContent.createForExternalFileLocation(file));
    }

    public static IASTTranslationUnit translationUnitForString(String string) throws CoreException {
        return translationUnitForFileContent(FileContent.create("test.c", string.toCharArray()));
    }

    private static IASTTranslationUnit translationUnitForFileContent(FileContent fileContent) throws CoreException {
        IParserLogService log = new DefaultLogService();
        Map<String, String> definedSymbols = new HashMap<String, String>();
        String[] includePaths = new String[0];
        IScannerInfo scanInfo = new ScannerInfo(definedSymbols, includePaths);
        IncludeFileContentProvider fileContentProvider = IncludeFileContentProvider.getEmptyFilesProvider();
        IASTTranslationUnit translationUnit = GCCLanguage.getDefault().getASTTranslationUnit(fileContent, scanInfo,
                fileContentProvider, null, 0, log);
        return translationUnit;
    }

    public static IASTStatement parseStatementNoFail(String string) {
        try {
            return parseStatement(string);
        } catch (CoreException e) {
            throw new IllegalStateException("INTERNAL ERROR: Could not parse " + string);
        }
    }

    public static IASTStatement parseStatement(String string) throws CoreException {
        String program = String.format("void f() { %s; }", string);
        IASTTranslationUnit tu = translationUnitForString(program);
        Assert.assertNotNull(tu);
        IASTStatement stmt = ASTUtil.findOne(tu, IASTStatement.class);
        Assert.assertNotNull(stmt);
        Assert.assertTrue(stmt instanceof IASTCompoundStatement);
        return ((IASTCompoundStatement) stmt).getStatements()[0];
    }

    public static IASTExpression parseExpressionNoFail(String string) {
        try {
            return parseExpression(string);
        } catch (CoreException e) {
            throw new IllegalStateException("INTERNAL ERROR: Could not parse " + string);
        }
    }

    public static IASTExpression parseExpression(String string) throws CoreException {
        IASTStatement stmt = parseStatement(string + ";");
        Assert.assertNotNull(stmt);
        Assert.assertTrue(stmt instanceof IASTExpressionStatement);
        return ((IASTExpressionStatement) stmt).getExpression();
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

    public static Pair<IASTExpression, IASTExpression> getAssignment(IASTExpression expr) {
        if (!(expr instanceof IASTBinaryExpression))
            return null;

        IASTBinaryExpression binExp = (IASTBinaryExpression) expr;
        if (binExp.getOperator() != IASTBinaryExpression.op_assign)
            return null;

        return new Pair<IASTExpression, IASTExpression>(binExp.getOperand1(), binExp.getOperand2());
    }

    public static IASTName getIdExpression(IASTExpression expr) {
        if (!(expr instanceof IASTIdExpression))
            return null;

        return ((IASTIdExpression) expr).getName();
    }

    @SuppressWarnings("unused")
    private static Pair<IASTName, LinearExpression> getSimpleArrayAccess(IASTExpression expr) {
        if (!(expr instanceof IASTArraySubscriptExpression))
            return null;

        IASTArraySubscriptExpression arrSub = (IASTArraySubscriptExpression) expr;
        IASTExpression array = arrSub.getArrayExpression();
        IASTInitializerClause subscript = arrSub.getArgument();

        IASTName name = getIdExpression(array);
        if (name == null || !(subscript instanceof IASTExpression))
            return null;

        LinearExpression linearSubscript = LinearExpression.createFrom((IASTExpression) subscript);

        return new Pair<IASTName, LinearExpression>(name, linearSubscript);
    }

    public static Pair<IASTName, LinearExpression[]> getMultidimArrayAccess(IASTExpression expr) {
        if (!(expr instanceof IASTArraySubscriptExpression))
            return null;

        IASTArraySubscriptExpression arrSub = (IASTArraySubscriptExpression) expr;
        IASTExpression array = arrSub.getArrayExpression();
        IASTInitializerClause subscript = arrSub.getArgument();

        IASTName name;
        LinearExpression[] prevSubscripts;
        if (array instanceof IASTArraySubscriptExpression) {
            Pair<IASTName, LinearExpression[]> nested = getMultidimArrayAccess(array);
            name = nested.getFirst();
            prevSubscripts = nested.getSecond();
        } else {
            name = getIdExpression(array);
            prevSubscripts = new LinearExpression[0];
        }

        if (name == null || !(subscript instanceof IASTExpression))
            return null;

        LinearExpression thisSubscript = LinearExpression.createFrom((IASTExpression) subscript);
        return new Pair<IASTName, LinearExpression[]>(name, concat(prevSubscripts, thisSubscript));
    }

    private static LinearExpression[] concat(LinearExpression[] prevSubscripts, LinearExpression thisSubscript) {
        // If any of the subscript expressions is not linear, treat the array access like a scalar access
        // (i.e., ignore all subscripts)
        if (prevSubscripts == null || thisSubscript == null)
            return null;

        LinearExpression[] result = new LinearExpression[prevSubscripts.length + 1];
        System.arraycopy(prevSubscripts, 0, result, 0, prevSubscripts.length);
        result[result.length - 1] = thisSubscript;
        return result;
    }

    public static Integer getConstantExpression(IASTExpression expr) {
        if (!(expr instanceof IASTLiteralExpression))
            return null;

        IASTLiteralExpression literal = (IASTLiteralExpression) expr;
        if (literal.getKind() != IASTLiteralExpression.lk_integer_constant)
            return null;

        try {
            return Integer.parseInt(String.valueOf(literal.getValue()));
        } catch (NumberFormatException e) {
            return null;
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
    public static void raise(String message, IASTNode node) {
        throw new RuntimeException(message + " at line " + node.getFileLocation().getStartingLineNumber());
    }

    private ASTUtil() {
    }
}
