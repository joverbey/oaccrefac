/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     Adam Eichelkraut (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.ToolFactory;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.formatter.CodeFormatter;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.junit.Assert;

import edu.auburn.oaccrefac.internal.core.dependence.LinearExpression;

public class ASTUtil {

    public static <T> List<T> find(IASTNode parent, Class<T> clazz) {
        List<T> results = new LinkedList<T>();
        findAndAdd(parent, clazz, results);
        return results;
    }

    public static <T> T findOne(IASTNode parent, Class<T> clazz) {
        List<T> results = find(parent, clazz);
        if (results.size() == 0) {
            return null;
        }
        return results.get(0);
    }

    public static <T> T findDepth(IASTNode parent, Class<T> clazz, int depth) {
        List<T> results = find(parent, clazz);
        if (results.size() == 0) {
            return null;
        }

        return results.get(depth);
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

    public static boolean isAncestor(IASTNode ancestor, IASTNode descendant) {
        for(IASTNode node = descendant; node != null; node = node.getParent()) {
            if(node.equals(ancestor)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * This method (which baffles me as to why there isn't one of these in the IASTNode class, but whatever) returns the
     * next sibling after itself with respect to its parent.
     * 
     * @param n
     *            node in which to find next sibling
     * @return IASTNode of next sibling or null if last child
     */
    public static IASTNode getNextSibling(IASTNode n) {
        if (n.getParent() != null) {
            IASTNode[] chilluns = n.getParent().getChildren();
            for (int i = 0; i < chilluns.length; i++) {
                if (n == chilluns[i] && i < (chilluns.length - 1)) {
                    return chilluns[i + 1];
                }
            }
        }
        return null;
    }

    public static boolean isNameInScope(IASTName varname, IScope scope) {
        IBinding[] bindings = scope.find(new String(varname.getSimpleID()));
        if (bindings.length > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNameInScope(String varname, IScope scope) {
        IBinding[] bindings = scope.find(varname);
        if (bindings.length > 0) {
            return true;
        } else {
            return false;
        }
    }
    
    public static List<IBinding> getLoopIndexVariables(List<IASTForStatement> loops) {
        List<IBinding> result = new ArrayList<IBinding>(loops.size());
        for (IASTForStatement forStmt : loops) {
            ForStatementInquisitor loop = InquisitorFactory.getInquisitor(forStmt);
            IBinding variable = loop.getIndexVariable();
            if (variable != null) {
                result.add(variable);
            }
        }
        return result;
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

    public static Pair<IASTName, IASTName> getSimpleFieldReference(IASTExpression expr) {
        if (!(expr instanceof IASTFieldReference))
            return null;

        IASTFieldReference fieldReference = (IASTFieldReference) expr;
        IASTName owner = getIdExpression(fieldReference.getFieldOwner());
        IASTName field = fieldReference.getFieldName();

        if (owner == null || field == null || fieldReference.isPointerDereference())
            return null;
        else
            return new Pair<IASTName, IASTName>(owner, field);
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
            if (nested == null)
                return null;
            name = nested.getFirst();
            prevSubscripts = nested.getSecond();
        } else {
            name = getIdExpression(array);
            if (name == null) {
                Pair<IASTName, IASTName> fieldRef = getSimpleFieldReference(array);
                name = fieldRef == null ? null : fieldRef.getSecond();
            }
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

    /**
     * Returns the (approximate) source code for an AST node, primarily for use in debugging.
     * 
     * @param node
     * @return String
     */
    public static String toString(IASTNode node) {
        try {
            StringBuilder sb = new StringBuilder();
            for (IToken tok = node.getSyntax(); tok != null; tok = tok.getNext()) {
                sb.append(tok.getCharImage());
                sb.append(' ');
            }
            return sb.toString().trim();
        } catch (ExpansionOverlapsBoundaryException e) {
            return "<error>";
        }
    }
    
    public static String format(String source) {
        CodeFormatter cf = ToolFactory.createDefaultCodeFormatter(null);
        TextEdit edit = cf.format(CodeFormatter.K_STATEMENTS, 
                source, 0, source.length(), 0, null);
        IDocument doc = new Document(source);
        try {
            edit.apply(doc);
        } catch (MalformedTreeException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return doc.get();
    }

    /**
     * Returns the first few characters of an AST node's source code, primarily for use in descriptive messages.
     * 
     * @param node
     * @return String
     */
    public static String summarize(IASTNode node) {
        final int MAX_LEN = 80;

        String result = toString(node).replace('\n', ' ');
        if (result.length() > MAX_LEN)
            result = result.substring(0, MAX_LEN + 1) + "...";
        return result;
    }
    
    private ASTUtil() {
    }
}
