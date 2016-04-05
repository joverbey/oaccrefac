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
package org.eclipse.ptp.pldt.openacc.internal.core.patternmatching;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

/**
 * Matches a subtree of an abstract syntax tree against a pattern.
 */
public final class ASTMatcher {

    private Map<String, String> nameMapping = new TreeMap<String, String>();

    /**
     * Receives an AST representing a pattern for a C statement or expressions, and matches the given tree against that
     * pattern, returning <code>null</code> if the tree does not match the pattern and a Map otherwise.
     * <p>
     * Identifiers in the pattern and the matched tree may differ, as long as there is a 1-1 mapping between identifiers
     * in the two trees. The returned map describes the mapping from identifiers in the pattern tree to those in the
     * given tree. For example, the pattern <code>for (i = 0; i < n; i++) ;</code> matches the tree for
     * <code>for (eye = 0; eye < enn; eye++) ;</code> with the map <code>{i=eye, n=enn}</code> returned.
     * <p>
     * The pattern may contain {@link ArbitraryStatement}, {@link ArbitraryExpression}, and
     * {@link ArbitraryIntegerConstant} nodes, which match arbitrary statements, expressions, and integer constants,
     * respectively.
     * 
     * @param pattern
     * @param tree
     * @return Map
     */
    public static Map<String, String> unify(IASTNode pattern, IASTNode tree) {
        ASTMatcher matcher = new ASTMatcher();
        if (matcher.genericMatch(pattern, tree))
            return matcher.nameMapping;
        else
            return null;
    }

    private boolean genericMatch(IASTNode pattern, IASTNode node) {
        if (pattern == null && node == null)
            return true;
        else if (pattern == null || node == null)
            return false;

        if (pattern instanceof ArbitraryIntegerConstant && node instanceof IASTLiteralExpression)
            return ((IASTLiteralExpression) node).getKind() == IASTLiteralExpression.lk_integer_constant;
        if (pattern instanceof ArbitraryExpression && node instanceof IASTExpression)
            return true;
        if (pattern instanceof ArbitraryStatement && node instanceof IASTStatement)
            return true;
        if (!pattern.getClass().equals(node.getClass()))
            return false;

        if (pattern instanceof IASTBreakStatement) {
            return match((IASTBreakStatement) pattern, (IASTBreakStatement) node);
        } else if (pattern instanceof IASTCaseStatement) {
            return match((IASTCaseStatement) pattern, (IASTCaseStatement) node);
        } else if (pattern instanceof IASTCompoundStatement) {
            return match((IASTCompoundStatement) pattern, (IASTCompoundStatement) node);
        } else if (pattern instanceof IASTContinueStatement) {
            return match((IASTContinueStatement) pattern, (IASTContinueStatement) node);
        } else if (pattern instanceof IASTEqualsInitializer) {
            return match((IASTEqualsInitializer) pattern, (IASTEqualsInitializer) node);
        } else if (pattern instanceof IASTDeclarator) {
            return match((IASTDeclarator) pattern, (IASTDeclarator) node);
        } else if (pattern instanceof IASTDeclSpecifier) {
            return match((IASTDeclSpecifier) pattern, (IASTDeclSpecifier) node);
        } else if (pattern instanceof IASTDeclaration) {
            return match((IASTDeclaration) pattern, (IASTDeclaration) node);
        } else if (pattern instanceof IASTDeclarationStatement) {
            return match((IASTDeclarationStatement) pattern, (IASTDeclarationStatement) node);
        } else if (pattern instanceof IASTDefaultStatement) {
            return match((IASTDefaultStatement) pattern, (IASTDefaultStatement) node);
        } else if (pattern instanceof IASTDoStatement) {
            return match((IASTDoStatement) pattern, (IASTDoStatement) node);
        } else if (pattern instanceof IASTExpressionStatement) {
            return match((IASTExpressionStatement) pattern, (IASTExpressionStatement) node);
        } else if (pattern instanceof IASTForStatement) {
            return match((IASTForStatement) pattern, (IASTForStatement) node);
        } else if (pattern instanceof IASTGotoStatement) {
            return match((IASTGotoStatement) pattern, (IASTGotoStatement) node);
        } else if (pattern instanceof IASTIfStatement) {
            return match((IASTIfStatement) pattern, (IASTIfStatement) node);
        } else if (pattern instanceof IASTLabelStatement) {
            return match((IASTLabelStatement) pattern, (IASTLabelStatement) node);
        } else if (pattern instanceof IASTNullStatement) {
            return match((IASTNullStatement) pattern, (IASTNullStatement) node);
        } else if (pattern instanceof IASTReturnStatement) {
            return match((IASTReturnStatement) pattern, (IASTReturnStatement) node);
        } else if (pattern instanceof IASTSwitchStatement) {
            return match((IASTSwitchStatement) pattern, (IASTSwitchStatement) node);
        } else if (pattern instanceof IASTWhileStatement) {
            return match((IASTWhileStatement) pattern, (IASTWhileStatement) node);
        } else if (pattern instanceof IASTBinaryExpression) {
            return match((IASTBinaryExpression) pattern, (IASTBinaryExpression) node);
        } else if (pattern instanceof IASTIdExpression) {
            return match((IASTIdExpression) pattern, (IASTIdExpression) node);
        } else if (pattern instanceof IASTLiteralExpression) {
            return match((IASTLiteralExpression) pattern, (IASTLiteralExpression) node);
        } else if (pattern instanceof IASTUnaryExpression) {
            return match((IASTUnaryExpression) pattern, (IASTUnaryExpression) node);
        } else if (pattern instanceof IASTCastExpression) {
            return match((IASTCastExpression) pattern, (IASTCastExpression) node);
        } else if (pattern instanceof IASTConditionalExpression) {
            return match((IASTConditionalExpression) pattern, (IASTConditionalExpression) node);
        } else if (pattern instanceof IASTArraySubscriptExpression) {
            return match((IASTArraySubscriptExpression) pattern, (IASTArraySubscriptExpression) node);
        } else if (pattern instanceof IASTFieldReference) {
            return match((IASTFieldReference) pattern, (IASTFieldReference) node);
        } else if (pattern instanceof IASTFunctionCallExpression) {
            return match((IASTFunctionCallExpression) pattern, (IASTFunctionCallExpression) node);
        } else if (pattern instanceof IASTExpressionList) {
            return match((IASTExpressionList) pattern, (IASTExpressionList) node);
        } else if (pattern instanceof IASTTypeIdExpression) {
            return match((IASTTypeIdExpression) pattern, (IASTTypeIdExpression) node);
        } else if (pattern instanceof IASTFieldReference) {
            return match((IASTFieldReference) pattern, (IASTFieldReference) node);
        } else if (pattern instanceof IASTName) {
            return match((IASTName) pattern, (IASTName) node);
        } else {
            return unsupported(pattern);
        }
    }

    private boolean unsupported(IASTNode pattern) {
        throw new UnsupportedOperationException("Pattern matching not implemented for "
                + pattern.getClass().getSimpleName());
    }

    public boolean match(IASTBreakStatement pattern, IASTBreakStatement node) {
        return true;
    }

    public boolean match(IASTCaseStatement pattern, IASTCaseStatement node) {
        return unsupported(pattern);
    }

    public boolean match(IASTDefaultStatement pattern, IASTDefaultStatement node) {
        return unsupported(pattern);
    }

    public boolean match(IASTCompoundStatement pattern, IASTCompoundStatement node) {
        IASTStatement[] patternStmts = pattern.getStatements();
        IASTStatement[] nodeStmts = node.getStatements();
        int len = patternStmts.length;
        if (nodeStmts.length != len)
            return false;
        for (int i = 0; i < len; i++) {
            if (!genericMatch(patternStmts[i], nodeStmts[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean match(IASTContinueStatement pattern, IASTContinueStatement node) {
        return true;
    }
    
    public boolean match (IASTEqualsInitializer pattern, IASTEqualsInitializer node) {
        return genericMatch(pattern.getInitializerClause(), node.getInitializerClause());
    }
    
    public boolean match(IASTDeclaration pattern, IASTDeclaration node) {
        IASTNode[] patternChilluns = pattern.getChildren();
        IASTNode[] nodeChilluns = node.getChildren();
        int len = patternChilluns.length;
        if (nodeChilluns.length != len)
            return false;
        for (int i = 0; i < len; i++) {
            if (!genericMatch(patternChilluns[i], nodeChilluns[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean match(IASTDeclarator pattern, IASTDeclarator node) {
        return genericMatch(pattern.getInitializer(), node.getInitializer())
             && genericMatch(pattern.getNestedDeclarator(), node.getNestedDeclarator());
    }
    
    public boolean match(IASTDeclSpecifier pattern, IASTDeclSpecifier node) {
        return (pattern.getStorageClass() == node.getStorageClass());
    }

    public boolean match(IASTDeclarationStatement pattern, IASTDeclarationStatement node) {
        return genericMatch(pattern.getDeclaration(), node.getDeclaration());
    }

    public boolean match(IASTDoStatement pattern, IASTDoStatement node) {
        return unsupported(pattern);
    }

    public boolean match(IASTExpressionStatement pattern, IASTExpressionStatement node) {
        return genericMatch(pattern.getExpression(), node.getExpression());
    }

    public boolean match(IASTForStatement pattern, IASTForStatement node) {
        return genericMatch(pattern.getInitializerStatement(), node.getInitializerStatement())
                && genericMatch(pattern.getConditionExpression(), node.getConditionExpression())
                && genericMatch(pattern.getIterationExpression(), node.getIterationExpression())
                && genericMatch(pattern.getBody(), node.getBody());
    }

    public boolean match(IASTGotoStatement pattern, IASTGotoStatement node) {
        return unsupported(pattern);
    }

    public boolean match(IASTIfStatement pattern, IASTIfStatement node) {
        return unsupported(pattern);
    }

    public boolean match(IASTLabelStatement pattern, IASTLabelStatement node) {
        return unsupported(pattern);
    }

    public boolean match(IASTNullStatement pattern, IASTNullStatement node) {
        return true;
    }

    public boolean match(IASTReturnStatement pattern, IASTReturnStatement node) {
        return unsupported(pattern);
    }

    public boolean match(IASTSwitchStatement pattern, IASTSwitchStatement node) {
        return unsupported(pattern);
    }

    public boolean match(IASTWhileStatement pattern, IASTWhileStatement node) {
        return unsupported(pattern);
    }

    private boolean match(IASTBinaryExpression pattern, IASTBinaryExpression node) {
        return pattern.getOperator() == node.getOperator() //
                && genericMatch(pattern.getOperand1(), node.getOperand1()) //
                && genericMatch(pattern.getOperand2(), node.getOperand2());
    }

    private boolean match(IASTIdExpression pattern, IASTIdExpression node) {
        return genericMatch(pattern.getName(), node.getName());
    }

    private boolean match(IASTName pattern, IASTName node) {
        String patternName = pattern.toString();
        String nodeName = node.toString();
        if (!nameMapping.containsKey(patternName))
            nameMapping.put(patternName, nodeName);
        return nodeName.equals(nameMapping.get(patternName));
    }

    private boolean match(IASTFieldReference pattern, IASTFieldReference node) {
        return pattern.isPointerDereference() == node.isPointerDereference()
                && genericMatch(pattern.getFieldOwner(), node.getFieldOwner())
                && genericMatch(pattern.getFieldName(), node.getFieldName());
    }

    private boolean match(IASTLiteralExpression pattern, IASTLiteralExpression node) {
        return pattern.getKind() == node.getKind() && Arrays.equals(pattern.getValue(), node.getValue());
    }

    private boolean match(IASTUnaryExpression pattern, IASTUnaryExpression node) {
        return pattern.getOperator() == node.getOperator() && genericMatch(pattern.getOperand(), node.getOperand());
    }

    private boolean match(IASTCastExpression pattern, IASTCastExpression node) {
        return unsupported(pattern);
    }

    private boolean match(IASTConditionalExpression pattern, IASTConditionalExpression node) {
        return unsupported(pattern);
    }

    private boolean match(IASTArraySubscriptExpression pattern, IASTArraySubscriptExpression node) {
        return unsupported(pattern);
    }

    private boolean match(IASTFunctionCallExpression pattern, IASTFunctionCallExpression node) {
        return unsupported(pattern);
    }

    private boolean match(IASTExpressionList pattern, IASTExpressionList node) {
        return unsupported(pattern);
    }

    private boolean match(IASTTypeIdExpression pattern, IASTTypeIdExpression node) {
        return unsupported(pattern);
    }

    private ASTMatcher() {
        ;
    }
}
