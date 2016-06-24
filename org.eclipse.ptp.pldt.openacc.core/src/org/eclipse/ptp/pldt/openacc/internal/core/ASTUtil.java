/*******************************************************************************
tat * Copyright (c) 2015, 2016 Auburn University and others.
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
package org.eclipse.ptp.pldt.openacc.internal.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.cdt.core.ToolFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunction;
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.FileStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class ASTUtil {

	public static final Comparator<IASTNode> REVERSE_COMPARATOR = new Comparator<IASTNode>() {

		@Override
		public int compare(IASTNode node1, IASTNode node2) {
			return node2.getFileLocation().getNodeOffset() - node1.getFileLocation().getNodeOffset();
		}

	};

	public static final Comparator<IASTNode> FORWARD_COMPARATOR = new Comparator<IASTNode>() {

		@Override
		public int compare(IASTNode node1, IASTNode node2) {
			return node1.getFileLocation().getNodeOffset() - node2.getFileLocation().getNodeOffset();
		}

	};

	public static <T> List<T> find(IASTNode parent, Class<T> clazz) {
		List<T> results = new LinkedList<T>();
		findAndAdd(parent, clazz, results);
		return results;
	}

	public static <T> T findFirst(IASTNode parent, Class<T> clazz) {
		List<T> results = find(parent, clazz);
		if (results.size() == 0) {
			return null;
		}
		return results.get(0);
	}

	public static <T> T findDepth(IASTNode parent, Class<T> clazz, int depth) {
		List<T> results = find(parent, clazz);
		if (results.size() <= depth) {
			return null;
		}
		return results.get(depth);
	}

	private static <T> void findAndAdd(IASTNode parent, Class<T> clazz, List<T> results) {
		if (clazz.isInstance(parent)) {
			results.add(clazz.cast(parent));
		}

		for (IASTNode child : parent.getChildren()) {
			findAndAdd(child, clazz, results);
		}
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
	
//	public static IASTNode findNearestAncestor(IASTNode startingNode, Class<?>... clazzes) {
//		for (IASTNode node = startingNode.getParent(); node != null; node = node.getParent()) {
//			for(Class<?> clazz : clazzes) {
//				if (clazz.isInstance(node)) {
//					return node;
//				}
//			}
//		}
//		return null;
//	}

	/**
	 * returns <code>true</code> if <code>ancestor</code> is an ancestor of or is the the same node as
	 * <code>descendant</code>
	 */
	public static boolean isAncestor(IASTNode descendant, IASTNode ancestor) {
		for (IASTNode node = descendant; node != null; node = node.getParent()) {
			if (node.equals(ancestor)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAncestor(IASTNode descendant, IASTNode... ancestors) {
		for (IASTNode ancestor : ancestors) {
			if (isAncestor(descendant, ancestor)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns <code>true</code> if <code>ancestor</code> is an ancestor of AND is the not the same node as
	 * <code>descendant</code>
	 */
	public static boolean isStrictAncestor(IASTNode descendant, IASTNode ancestor) {
		for (IASTNode node = descendant.getParent(); node != null; node = node.getParent()) {
			if (node.equals(ancestor)) {
				return true;
			}
		}
		return false;
	}

	public static boolean doesNodeLexicallyContain(IASTNode container, IASTNode containee) {
		// return (start of container >= start of containee && end of container <= end of containee)
		return container.getFileLocation().getNodeOffset() <= containee.getFileLocation().getNodeOffset()
				&& container.getFileLocation().getNodeOffset()
						+ container.getFileLocation().getNodeLength() >= containee.getFileLocation().getNodeOffset()
								+ containee.getFileLocation().getNodeLength();
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
			IASTNode[] children = n.getParent().getChildren();
			for (int i = 0; i < children.length; i++) {
				if (n == children[i] && i < (children.length - 1)) {
					return children[i + 1];
				}
			}
		}
		return null;
	}

	public static IASTNode getPreviousSibling(IASTNode n) {
		if (n.getParent() != null) {
			IASTNode[] children = n.getParent().getChildren();
			for (int i = 0; i < children.length; i++) {
				if (n == children[i] && i > 0) {
					return children[i - 1];
				}
			}
		}
		return null;
	}

	// FIXME if two variables have the same name, this always returns the same thing for both for any scope
	// should check based on the name's binding somehow
	public static boolean isNameInScope(IASTName varname, IScope scope) {
		return isNameInScope(new String(varname.getSimpleID()), scope);
	}

	// return true if a variable by the given name exists in the scope
	public static boolean isNameInScope(String varname, IScope scope) {
		return scope.find(varname).length > 0;
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
		if (tu == null)
			throw new CoreException(Status.CANCEL_STATUS);
		IASTStatement stmt = ASTUtil.findFirst(tu, IASTStatement.class);
		if (stmt == null || !(stmt instanceof IASTCompoundStatement))
			throw new CoreException(Status.CANCEL_STATUS);
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
		if (stmt == null || !(stmt instanceof IASTExpressionStatement))
			throw new CoreException(Status.CANCEL_STATUS);
		return ((IASTExpressionStatement) stmt).getExpression();
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
		TextEdit edit = cf.format(CodeFormatter.K_STATEMENTS, source, 0, source.length(), 0, null);
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

	public static String[] getPragmaStrings(IASTStatement statement) {
		List<IASTPreprocessorPragmaStatement> p = getPragmaNodes(statement);
		String[] pragCode = new String[p.size()];
		for (int i = 0; i < pragCode.length; i++) {
			pragCode[i] = p.get(i).getRawSignature();
		}
		return pragCode;
	}

	public static List<IASTPreprocessorPragmaStatement> getPragmaNodes(IASTNode statement) {
		int loopLoc = statement.getFileLocation().getNodeOffset();
		int precedingStmtOffset = getNearestPrecedingStatementOffset(statement);
		List<IASTPreprocessorPragmaStatement> pragmas = new ArrayList<IASTPreprocessorPragmaStatement>();
		for (IASTPreprocessorStatement pre : statement.getTranslationUnit().getAllPreprocessorStatements()) {
			if (pre instanceof IASTPreprocessorPragmaStatement
					&& ((IASTPreprocessorPragmaStatement) pre).getFileLocation().getNodeOffset() < loopLoc
					&& ((IASTPreprocessorPragmaStatement) pre).getFileLocation()
							.getNodeOffset() > precedingStmtOffset) {
				pragmas.add((IASTPreprocessorPragmaStatement) pre);
			}
		}
		Collections.sort(pragmas, ASTUtil.FORWARD_COMPARATOR);
		return pragmas;
	}
	
	public static List<IASTPreprocessorPragmaStatement> getInternalPragmaNodes(IASTStatement statement) {
		List<IASTPreprocessorPragmaStatement> pragmas = new ArrayList<IASTPreprocessorPragmaStatement>();
		for (IASTPreprocessorStatement pre : statement.getTranslationUnit().getAllPreprocessorStatements()) {
			if (pre instanceof IASTPreprocessorPragmaStatement && doesNodeLexicallyContain(statement, pre)) {
				pragmas.add((IASTPreprocessorPragmaStatement) pre);
			}
		}
		return pragmas;
	}


    public static Map<IASTPreprocessorPragmaStatement, IASTNode> getEnclosingPragmas(IASTStatement statement) {
        Map<IASTPreprocessorPragmaStatement, IASTNode> pragmas = new TreeMap<>(ASTUtil.FORWARD_COMPARATOR);

        IASTPreprocessorStatement[] preprocessorStatements = statement.getTranslationUnit().getAllPreprocessorStatements();
        
        for (IASTNode node = statement; node != null; node = node.getParent()) {
            int location = node.getFileLocation().getNodeOffset();
            
            if (node instanceof IASTStatement) {
	            int precedingStmtOffset = getNearestPrecedingStatementOffset((IASTStatement) node);
	            
	            for (IASTPreprocessorStatement pre : preprocessorStatements) {
	                if (pre instanceof IASTPreprocessorPragmaStatement
	                        && ((IASTPreprocessorPragmaStatement) pre).getFileLocation().getNodeOffset() < location
	                        && ((IASTPreprocessorPragmaStatement) pre).getFileLocation()
	                                .getNodeOffset() > precedingStmtOffset) {
	                    pragmas.put((IASTPreprocessorPragmaStatement) pre, node);
	                }
	            }
            }
        }
        return pragmas;
    }

	private static int getNearestPrecedingStatementOffset(IASTNode stmt) {

		class OffsetFinder extends ASTVisitor {

			// the offset of the nearest lexical predecessor of the given node
			int finalOffset;
			int thisOffset;

			public OffsetFinder(int offset) {
				shouldVisitStatements = true;
				shouldVisitDeclarations = true;
				this.thisOffset = offset;
			}

			@Override
			public int visit(IASTStatement stmt) {
				int foundOffset = stmt.getFileLocation().getNodeOffset();
				if (thisOffset - foundOffset < thisOffset - finalOffset && foundOffset < thisOffset) {
					this.finalOffset = foundOffset;
				}
				return PROCESS_CONTINUE;
			}

			@Override
			public int visit(IASTDeclaration dec) {
				int foundOffset = dec.getFileLocation().getNodeOffset();
				if (thisOffset - foundOffset < thisOffset - finalOffset && foundOffset < thisOffset) {
					this.finalOffset = foundOffset;
				}
				return PROCESS_CONTINUE;
			}

		}

		OffsetFinder finder = new OffsetFinder(stmt.getFileLocation().getNodeOffset());
		IASTNode containingNode = ASTUtil.findNearestAncestor(stmt, IASTNode.class);
		containingNode.accept(finder);
		return finder.finalOffset;
	}

	public static List<IASTComment> getLeadingComments(IASTStatement statement) {
		int stmtOffset = statement.getFileLocation().getNodeOffset();
		List<IASTComment> comments = new ArrayList<IASTComment>();
		IASTNode previousSibling = ASTUtil.getPreviousSibling(statement);
		if (previousSibling != null) {
			int precedingEnd = previousSibling.getFileLocation().getNodeOffset()
					+ previousSibling.getFileLocation().getNodeLength();

			for (IASTComment comment : statement.getTranslationUnit().getComments()) {
				if (comment.getFileLocation().getNodeOffset() < stmtOffset
						&& comment.getFileLocation().getNodeOffset() > precedingEnd) {
					comments.add(comment);
				}
			}

		} else {
			for (IASTComment comment : statement.getTranslationUnit().getComments()) {
				int comStart = comment.getFileLocation().getNodeOffset();
				if (comStart < stmtOffset && comStart > statement.getParent().getFileLocation().getNodeOffset()) {
					// comment.start
					comments.add(comment);
				}
			}
		}

		Collections.sort(comments, ASTUtil.FORWARD_COMPARATOR);
		return comments;
	}

	public static IASTStatement[] getStatementsIfCompound(IASTStatement statement) {
		if (statement instanceof IASTCompoundStatement) {
			return ((IASTCompoundStatement) statement).getStatements();
		}
		return new IASTStatement[] { statement };
	}

	public static boolean doesConstructContainAllReferencesToVariablesItDeclares(IASTStatement... construct) {
		IASTFunctionDefinition func = ASTUtil.findNearestAncestor(construct[0], IASTFunctionDefinition.class);
    	List<IASTName> varOccurrences = ASTUtil.find(func, IASTName.class);
    	Set<IASTDeclarator> declaratorsInConstruct = new HashSet<IASTDeclarator>();
    	for(IASTStatement stmt : construct) {
    		for(IASTSimpleDeclaration declaration : ASTUtil.find(stmt, IASTSimpleDeclaration.class)) {
    			//if declarator.getName().resolveBinding() occurs outside the construct
    			declaratorsInConstruct.addAll(Arrays.asList(declaration.getDeclarators()));
        	}
    	}
    	for(IASTDeclarator declarator : declaratorsInConstruct) {
    		for(IASTName occurrence : varOccurrences) {
    			//if a variable is declared in the construct and this is some reference to it
    			if(declarator.getName().resolveBinding().equals(occurrence.resolveBinding())) {
    				if(!ASTUtil.isAncestor(occurrence, construct)) {
    					return false;
    				}
    			}
    		}
    	}
    	return true;
	}
	

	public static IASTFunctionDefinition findFunctionDefinition(IASTFunctionCallExpression call) {
		if (!(call.getFunctionNameExpression() instanceof IASTIdExpression)) {
			return null;
		}
		IASTName callName = ((IASTIdExpression) call.getFunctionNameExpression()).getName();
		
		IBinding binding = callName.resolveBinding();
		if (!(binding instanceof IFunction)) {
			return null;
		}
		
		List<IASTFunctionDefinition> functionDefinitions = 
				find(call.getTranslationUnit(), IASTFunctionDefinition.class);
		for (IASTFunctionDefinition definition : functionDefinitions) {
			IASTName definitionName = definition.getDeclarator().getName();
			if (definitionName.toString().equals(callName.toString())) {
				return definition;
			}
		}
		return null;
	}
	
	public static RefactoringStatusContext getStatusContext(IASTNode node1, IASTNode node2) {
	    if (node1.getTranslationUnit() != node2.getTranslationUnit()) {
	        return null;
	    }
	    
	    String filename = node1.getTranslationUnit().getFileLocation().getFileName();
	    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(filename));
	    if (file == null) {
	        return null;
	    }
	
	    IASTFileLocation fileLocation1 = node1.getFileLocation();
	    IASTFileLocation fileLocation2 = node2.getFileLocation();
	    int start1 = fileLocation1.getNodeOffset();
	    int end1 = start1 + fileLocation1.getNodeLength();
	    int start2 = fileLocation2.getNodeOffset();
	    int end2 = start2 + fileLocation2.getNodeLength();
	    int start = Math.min(start1, start2);
	    int end = Math.max(end1, end2);
	    return new FileStatusContext(file, new Region(start, end - start));
	}

	private ASTUtil() {
	}

	
}
