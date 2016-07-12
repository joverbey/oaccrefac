/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     William Hester (Auburn) - initial API and implementation
 *     Alexander Calvert (Auburn) - added typedef consideration
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
import org.eclipse.cdt.core.dom.ast.ITypedef;
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
            "v = x;" //$NON-NLS-1$
    };

    private static final String[] WRITE_PATTERNS = {
            "x = expr;" //$NON-NLS-1$
    };

    private static final String[] UPDATE_PATTERNS = {
            "x++;", "x--;", "++x;", "--x;", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "x += expr;", "x -= expr;", "x *= expr;", "x /= expr;", "x %= expr", "x <<= expr;", "x >>= expr;", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
            "x = x + expr;", "x = x - expr;", "x = x * expr;", "x = x / expr;", "x = x % expr;", "x = x << expr;", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                    "x = x >> expr;", //$NON-NLS-1$
            "x = expr + x;", "x = expr - x;", "x = expr * x;", "x = expr / x;", "x = expr % x;", "x = expr << x;", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                    "x = expr >> x;", //$NON-NLS-1$
    };

    private static final String[] CAPTURE_PATTERNS = {
            "{ v = x; x += expr; }", //$NON-NLS-1$
            "{ x += expr; v = x; }", //$NON-NLS-1$
            "{ v = x; x++; }", //$NON-NLS-1$
            "{ x++; v = x; }", //$NON-NLS-1$
            "{ v = x; x = x + expr; }", //$NON-NLS-1$
            "{ x = x + expr; v = x; }", //$NON-NLS-1$
            "{ v = x; x = expr + x; }", //$NON-NLS-1$
            "{ x = expr + x; v = x; }", //$NON-NLS-1$
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
                    unaryExpression.setOperand(new ArbitraryLValue("x")); //$NON-NLS-1$
                }
                return PROCESS_CONTINUE;
            }
            
            @Override
            public int visit(IASTName name) {
                if (name.getParent() instanceof IASTIdExpression 
                        && name.getParent().getParent() instanceof IASTBinaryExpression) {
                    IASTIdExpression id = (IASTIdExpression) name.getParent();
                    IASTBinaryExpression binary = (IASTBinaryExpression) id.getParent();
                    if (name.toString().equals("expr")) { //$NON-NLS-1$
                        if (binary.getOperand1().equals(id)) {
                            binary.setOperand1(new ArbitraryExpression());
                        } else if (binary.getOperand2().equals(id)) {
                            binary.setOperand2(new ArbitraryExpression());
                        }
                    } else if (name.toString().equals("x")) { //$NON-NLS-1$
                        if (binary.getOperand1().equals(id)) {
                            binary.setOperand1(new ArbitraryLValue("x")); //$NON-NLS-1$
                        } else if (binary.getOperand2().equals(id)) {
                            binary.setOperand2(new ArbitraryLValue("x")); //$NON-NLS-1$
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
            binary.setOperand1(new ArbitraryLValue("v")); //$NON-NLS-1$
            binary.setOperand2(new ArbitraryLValue("x")); //$NON-NLS-1$
            
            ASTMatcher matcher = ASTMatcher.unifyWithMatcher(patternAST, statement);
            if (matcher != null) {
                readMapping = matcher.getNameMapping();
                readType = matcher.getArbitraryTypeBindings().get("x"); //$NON-NLS-1$
                break;
            }
        }
        
        Map<String, String> writeMapping = null;
        IType writeType = null;
        for (String pattern : WRITE_PATTERNS) {
            IASTExpressionStatement orig = (IASTExpressionStatement) ASTUtil.parseStatementNoFail(pattern);
            IASTExpressionStatement patternAST = orig.copy(CopyStyle.withoutLocations);
            IASTBinaryExpression binary = (IASTBinaryExpression) patternAST.getExpression();
            binary.setOperand1(new ArbitraryLValue("x")); //$NON-NLS-1$
            binary.setOperand2(new ArbitraryExpression());
            
            ASTMatcher matcher = ASTMatcher.unifyWithMatcher(patternAST, statement);
            if (matcher != null) {
                writeMapping = matcher.getNameMapping();
                writeType = matcher.getArbitraryTypeBindings().get("x"); //$NON-NLS-1$
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
                updateType = matcher.getArbitraryTypeBindings().get("x"); //$NON-NLS-1$
                break;
            }
        }

        if (readMapping != null && isDeclaredInNode(readMapping.get("v"), parallelRegion) //$NON-NLS-1$
                && !isDeclaredInNode(readMapping.get("x"), parallelRegion) //$NON-NLS-1$
                && isScalarType(readType)) {
            type = READ;
        } else if (updateMapping != null && !isDeclaredInNode(updateMapping.get("x"), parallelRegion) //$NON-NLS-1$
                && isScalarType(updateType)) {
            type = UPDATE;
        } else if (writeMapping != null && !isDeclaredInNode(writeMapping.get("x"), parallelRegion) //$NON-NLS-1$
                && isScalarType(writeType)) {
            type = WRITE;
        } else {
            type = NONE;
        }
    }
    
    private static boolean isScalarType(IType type) {
    	if(type instanceof ITypedef) {
    		return ((ITypedef) type).getType() instanceof IBasicType || ((ITypedef) type).getType() instanceof IPointerType;
    	}
    	else {
    		return type instanceof IBasicType || type instanceof IPointerType;
    	}
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
