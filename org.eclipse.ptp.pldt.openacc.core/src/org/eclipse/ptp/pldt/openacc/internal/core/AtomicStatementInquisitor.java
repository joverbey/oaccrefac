/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Auburn University - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ASTMatcher;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryExpression;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryLValue;

@SuppressWarnings("unused")
public final class AtomicStatementInquisitor {
    
    public static final int NONE = 0;
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int UPDATE = 3;

    private static final String[] READ_PATTERNS = {
            "v = x;"
    };

    private static final String[] WRITE_PATTERNS = {
            "x = expr;"
    };

    private static final String[] UPDATE_PATTERNS = {
            "x++;", "x--;", "++x;", "--x;",
            "x += expr;", "x -= expr;", "x *= expr;", "x /= expr;", "x %= expr", "x <<= expr;", "x >>= expr;",
            "x = x + expr;", "x = x - expr;", "x = x * expr;", "x = x / expr;", "x = x % expr;", "x = x << expr;",
                    "x = x >> expr;",
            "x = expr + x;", "x = expr - x;", "x = expr * x;", "x = expr / x;", "x = expr % x;", "x = expr << x;",
                    "x = expr >> x;",
    };

    private static final String[] CAPTURE_PATTERNS = {
            "{ v = x; x += expr; }",
            "{ x += expr; v = x; }",
            "{ v = x; x++; }",
            "{ x++; v = x; }",
            "{ v = x; x = x + expr; }",
            "{ x = x + expr; v = x; }",
            "{ v = x; x = expr + x; }",
            "{ x = expr + x; v = x; }",
    };

    private final int type;
    
    public static AtomicStatementInquisitor newInstance(IASTStatement statement, IASTNode parallelRegion) {
        return new AtomicStatementInquisitor(statement, parallelRegion);
    }
    
    private AtomicStatementInquisitor(IASTStatement statement, IASTNode parallelRegion) {
        class ExprReplacer extends ASTVisitor {
            public ExprReplacer() {
                shouldVisitExpressions = true;
                shouldVisitNames = true;
            }

            @Override
            public int visit(IASTExpression expr) {
                if (expr.getParent() instanceof IASTUnaryExpression) {
                    IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expr.getParent();
                    unaryExpression.setOperand(new ArbitraryLValue("x"));
                }
                return PROCESS_CONTINUE;
            }
            
            @Override
            public int visit(IASTName name) {
                if (name.getParent() instanceof IASTIdExpression 
                        && name.getParent().getParent() instanceof IASTBinaryExpression) {
                    IASTIdExpression id = (IASTIdExpression) name.getParent();
                    IASTBinaryExpression binary = (IASTBinaryExpression) id.getParent();
                    if (name.toString().equals("expr")) {
                        if (binary.getOperand1().equals(id)) {
                            binary.setOperand1(new ArbitraryExpression());
                        } else if (binary.getOperand2().equals(id)) {
                            binary.setOperand2(new ArbitraryExpression());
                        }
                    } else if (name.toString().equals("x")) {
                        if (binary.getOperand1().equals(id)) {
                            binary.setOperand1(new ArbitraryLValue("x"));
                        } else if (binary.getOperand2().equals(id)) {
                            binary.setOperand2(new ArbitraryLValue("x"));
                        }
                    }
                }
                return PROCESS_CONTINUE;
            }
        }
        
        Map<String, String> readMapping = null;
        IType readType = null;
        for (String pattern : READ_PATTERNS) {
            IASTExpressionStatement orig = (IASTExpressionStatement) ASTUtil.parseStatementNoFail(pattern);
            IASTExpressionStatement patternAST = orig.copy(CopyStyle.withoutLocations);
            IASTBinaryExpression binary = (IASTBinaryExpression) patternAST.getExpression();
            binary.setOperand1(new ArbitraryLValue("v"));
            binary.setOperand2(new ArbitraryLValue("x"));
            
            ASTMatcher matcher = ASTMatcher.unifyWithMatcher(patternAST, statement);
            if (matcher != null) {
                readMapping = matcher.getNameMapping();
                readType = matcher.getArbitraryTypeBindings().get("x");
                break;
            }
        }
        
        Map<String, String> writeMapping = null;
        IType writeType = null;
        for (String pattern : WRITE_PATTERNS) {
            IASTExpressionStatement orig = (IASTExpressionStatement) ASTUtil.parseStatementNoFail(pattern);
            IASTExpressionStatement patternAST = orig.copy(CopyStyle.withoutLocations);
            IASTBinaryExpression binary = (IASTBinaryExpression) patternAST.getExpression();
            binary.setOperand1(new ArbitraryLValue("x"));
            binary.setOperand2(new ArbitraryExpression());
            
            ASTMatcher matcher = ASTMatcher.unifyWithMatcher(patternAST, statement);
            if (matcher != null) {
                writeMapping = matcher.getNameMapping();
                writeType = matcher.getArbitraryTypeBindings().get("x");
                break;
            }
        }
        
        Map<String, String> updateMapping = null;
        IType updateType = null;
        for (String pattern : UPDATE_PATTERNS) {
            IASTExpressionStatement orig = (IASTExpressionStatement) ASTUtil.parseStatementNoFail(pattern);
            IASTExpressionStatement patternAST = orig.copy(CopyStyle.withoutLocations);
            patternAST.accept(new ExprReplacer());

            ASTMatcher matcher = ASTMatcher.unifyWithMatcher(patternAST, statement);
            if (matcher != null) {
                updateMapping = matcher.getNameMapping();
                updateType = matcher.getArbitraryTypeBindings().get("x");
                break;
            }
        }

        if (readMapping != null && isDeclaredInNode(readMapping.get("v"), parallelRegion)
                && !isDeclaredInNode(readMapping.get("x"), parallelRegion)
                && isScalarType(readType)) {
            type = READ;
        } else if (updateMapping != null && !isDeclaredInNode(updateMapping.get("x"), parallelRegion)
                && isScalarType(updateType)) {
            type = UPDATE;
        } else if (writeMapping != null && !isDeclaredInNode(writeMapping.get("x"), parallelRegion)
                && isScalarType(writeType)) {
            type = WRITE;
        } else {
            type = NONE;
        }
    }
    
    private static boolean isScalarType(IType type) {
        return type instanceof IBasicType || type instanceof IPointerType;
    }
    
    public static boolean isDeclaredInNode(String name, IASTNode node) {
        if (node instanceof IASTDeclarationStatement) {
            IASTDeclarationStatement declarationStatement = (IASTDeclarationStatement) node;
            IASTDeclaration declaration = declarationStatement.getDeclaration();
            if (declaration instanceof IASTSimpleDeclaration) {
                IASTSimpleDeclaration simple = (IASTSimpleDeclaration) declaration;
                IASTDeclarator[] declarators = simple.getDeclarators();
                for (IASTDeclarator declarator : declarators) {
                    if (declarator.getName().toString().equals(name)) {
                        return true;
                    }
                }
                return false;                
            }
            return false;
        }
        for (IASTNode n : node.getChildren()) {
            if (isDeclaredInNode(name, n)) {
                return true;
            }
        }
        return false;
    }
    
    public int getType() {
        return type;
    }

    private boolean checkUnaryUpdate(IASTStatement statement) {
        return statement instanceof IASTUnaryExpression;
    }
}
