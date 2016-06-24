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
package org.eclipse.ptp.pldt.internal.tests.analyses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.ptp.pldt.openacc.core.dataflow.Global;
import org.eclipse.ptp.pldt.openacc.core.dataflow.ReachingDefinitions;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

import junit.framework.TestCase;

public class ReachingDefinitionsTest extends TestCase {
    
    public void testBasic() throws Exception {
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {    \n" //1
                + "    int a = 10;  \n" //2
                + "    int b = 20;  \n" //3
                + "    a = b;       \n" //4
                + "    b = a;       \n" //5
                + "    a = 12;      \n" //6
                + "    a = 14;      \n" //7
                + "    b = a + 1;   \n" //8
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "int a = 10;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "int b = 20;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "b", 4, null));
                assertTrue(ru.size() == 1);
                break;
            case "a = b;":
                assertTrue(contains(rd, "b", 3, null));
                assertTrue(rd.size() == 1);
                assertTrue(contains(ru, "a", 5, null));
                assertTrue(ru.size() == 1);
                break;
            case "b = a;":
                assertTrue(contains(rd, "a", 4, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
                break;
            case "a = 12;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "a = 14;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(ru.size() == 1);
                break;
            case "b = a + 1;":
                assertTrue(contains(rd, "a", 7, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
                break;
            }
        }
    }
    
    public void testDeclaration() throws Exception {
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {    \n" //1
                + "    int a;       \n" //2
                + "    int b;       \n" //3
                + "    a = b;       \n" //4
                + "    b = a;       \n" //5
                + "    a = 12;      \n" //6
                + "    a = 14;      \n" //7
                + "    b = a + 1;   \n" //8
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "int a;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "int b;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "b", 4, null));
                assertTrue(ru.size() == 1);
                break;
            case "a = b;":
                assertTrue(contains(rd, "b", 3, null));
                assertTrue(rd.size() == 1);
                assertTrue(contains(ru, "a", 5, null));
                assertTrue(ru.size() == 1);
                break;
            case "b = a;":
                assertTrue(contains(rd, "a", 4, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
                break;
            case "a = 12;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "a = 14;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(ru.size() == 1);
                break;
            case "b = a + 1;":
                assertTrue(contains(rd, "a", 7, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
                break;
            }
        }
    }
    
    public void testBasicArray() throws Exception {
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                \n" //1
                + "    int a[3] = {1, 2, 3};    \n" //2
                + "    int b[2] = {4, 5};       \n" //3
                + "    a[1] = b[1];             \n" //4
                + "    b = a;                   \n" //5
                + "    a[2] = 12;               \n" //6
                + "    a[1] = 14;               \n" //7
                + "    b[0] = a[2] + 1;         \n" //8
                + "    a[0] = b[2] + 2;         \n" //9
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "int a[3] = {1, 2, 3};":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 5, null));
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(ru.size() == 2);
                break;
            case "int b[2] = {4, 5};":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "b", 4, null));
                assertTrue(ru.size() == 1);
                break;
            case "a[1] = b[1];":
                assertTrue(contains(rd, "b", 3, null));
                assertTrue(rd.size() == 1);
                assertTrue(contains(ru, "a", 5, null));
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(ru.size() == 2);
                break;
            case "b = a;":
            	assertTrue(contains(rd, "a", 2, null));
            	assertTrue(contains(rd, "a", 4, null));
                assertTrue(rd.size() == 2);
                assertTrue(contains(ru, "b", 9, null));
                assertTrue(ru.size() == 1);
                break;
            case "a[2] = 12;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(ru.size() == 1);
                break;
            case "a[1] = 14;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(ru.size() == 1);
                break;
            case "b[0] = a[1] + 1;":
            	assertTrue(contains(rd, "a", 2, null));
            	assertTrue(contains(rd, "a", 4, null));
            	assertTrue(contains(rd, "a", 6, null));
                assertTrue(contains(rd, "a", 7, null));
                assertTrue(rd.size() == 4);
                assertTrue(contains(ru, "b", 9, null));
                assertTrue(ru.size() == 1);
                break;
            case "a[0] = b[2] + 2;":
            	assertTrue(contains(rd, "b", 5, null));
                assertTrue(contains(rd, "b", 8, null));
            	assertTrue(rd.size() == 2);
                assertTrue(ru.isEmpty());
                break;
            }
        }
    }
    
    public void testForLoop() throws Exception {
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {            \n" //1
                + "    int a;               \n" //2
                + "    int b;               \n" //3
                + "    for(int i = 0;       \n" //4
                + "       i < 10;           \n" //5
                + "       i++) {            \n" //6
                + "        a = b;           \n" //7
                + "        b = a;           \n" //8
                + "        a = i;           \n" //9
                + "        a = 14;          \n" //10
                + "        b = a + i;       \n" //11
                + "    }"
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "int a;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "int b;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "b", 7, null));
                assertTrue(ru.size() == 1);
                break;
            case "a = b;":
                assertTrue(contains(rd, "b", 3, null));
                assertTrue(contains(rd, "b", 11, null));
                assertTrue(rd.size() == 2);
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(ru.size() == 1);
                break;
            case "b = a;":
                assertTrue(contains(rd, "a", 7, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
                break;
            case "a = i;":
                assertTrue(contains(rd, "i", 6, null));
                assertTrue(contains(rd, "i", 4, null));
                assertTrue(rd.size() == 2);
                assertTrue(ru.isEmpty());
                break;
            case "a = 14;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 11, null));
                assertTrue(ru.size() == 1);
                break;
            case "b = a + i;":
                assertTrue(contains(rd, "a", 10, null));
                assertTrue(contains(rd, "i", 4, null));
                assertTrue(contains(rd, "i", 6, null));
                assertTrue(rd.size() == 3);
                assertTrue(contains(ru, "b", 7, null));
                assertTrue(ru.size() == 1);
                break;
            case "int i = 0;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "i", 5, null));
                assertTrue(contains(ru, "i", 9, null));
                assertTrue(contains(ru, "i", 11, null));
                assertTrue(ru.size() == 3);
                break;
            case "i++":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "i", 5, null));
                assertTrue(contains(ru, "i", 9, null));
                assertTrue(contains(ru, "i", 11, null));
                assertTrue(ru.size() == 3);
                break;
            }
        }
    }
    
    public void testForLoopArray() throws Exception {
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                \n" //1
                + "    int a[10];               \n" //2
                + "    int b[20];               \n" //3
                + "    for(int i = 0;           \n" //4
                + "       i < 10;               \n" //5
                + "       i++) {                \n" //6
                + "        a[0] = b[6];         \n" //7
                + "        b[2] = a[9];         \n" //8
                + "        a[3] = i;            \n" //9
                + "        a[i] = 14;           \n" //10
                + "        b[5] = a[6] + i;     \n" //11
                + "    }"
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "int a[10];":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(contains(ru, "a", 11, null));
                assertTrue(ru.size() == 2);
                break;
            case "int b[20];":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "b", 7, null));
                assertTrue(ru.size() == 1);
                break;
            case "a[0] = b[6];":
                assertTrue(contains(rd, "b", 3, null));
                assertTrue(contains(rd, "b", 8, null));
                assertTrue(contains(rd, "b", 11, null));
                assertTrue(rd.size() == 3);
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(contains(ru, "a", 11, null));
                assertTrue(ru.size() == 2);
                break;
            case "b[2] = a[9];":
            	assertTrue(contains(rd, "a", 2, null));
                assertTrue(contains(rd, "a", 7, null));
                assertTrue(contains(rd, "a", 9, null));
                assertTrue(contains(rd, "a", 10, null));
                assertTrue(rd.size() == 4);
                assertTrue(contains(ru, "b", 7, null));
                assertTrue(ru.size() == 1);
                break;
            case "a[3] = i;":
                assertTrue(contains(rd, "i", 4, null));
                assertTrue(contains(rd, "i", 6, null));
                assertTrue(rd.size() == 2);
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(contains(ru, "a", 11, null));
                assertTrue(ru.size() == 2);
                break;
            case "a[i] = 14;":
                assertTrue(contains(rd, "i", 4, null));
                assertTrue(contains(rd, "i", 6, null));
                assertTrue(rd.size() == 2);
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(contains(ru, "a", 11, null));
                assertTrue(ru.size() == 2);
                break;
            case "b[5] = a[6] + i;":
            	assertTrue(contains(rd, "a", 2, null));
            	assertTrue(contains(rd, "a", 7, null));
            	assertTrue(contains(rd, "a", 9, null));
            	assertTrue(contains(rd, "a", 10, null));
                assertTrue(contains(rd, "i", 4, null));
                assertTrue(contains(rd, "i", 6, null));
                assertTrue(rd.size() == 6);
                assertTrue(contains(ru, "b", 7, null));
                assertTrue(ru.size() == 1);
                break;
            case "int i = 0;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "i", 5, null));
                assertTrue(contains(ru, "i", 9, null));
                assertTrue(contains(ru, "i", 10, null));
                assertTrue(contains(ru, "i", 11, null));
                assertTrue(ru.size() == 4);
                break;
            case "i++":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "i", 5, null));
                assertTrue(contains(ru, "i", 9, null));
                assertTrue(contains(ru, "i", 10, null));
                assertTrue(contains(ru, "i", 11, null));
                assertTrue(ru.size() == 4);
                break;
            }
        }
    }
    
    public void testWhileLoop() throws Exception {
        List<Integer> cases = new ArrayList<Integer>();
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {        \n" //1
                + "    int a = 10;      \n" //2
                + "    int b = 20;      \n" //3
                + "    while(a < 12) {  \n" //4
                + "        a = b;       \n" //5
                + "        b = a;       \n" //6
                + "        a = 12;      \n" //7
                + "        a = 14;      \n" //8
                + "    }                \n" //9
                + "    b = a + 1;       \n" //10
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "int a = 10;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 4, null));
                assertTrue(contains(ru, "a", 10, null));
                assertTrue(ru.size() == 2);
                break;
            case "int b = 20;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "b", 5, null));
                assertTrue(ru.size() == 1);
                break;
            case "a < 12":
                assertTrue(contains(rd, "a", 2, null));
                assertTrue(contains(rd, "a", 8, null));
                assertTrue(rd.size() == 2);
                assertTrue(ru.isEmpty());
                break;
            case "a = b;":
                assertTrue(contains(rd, "b", 3, null));
                assertTrue(contains(rd, "b", 6, null));
                assertTrue(rd.size() == 2);
                assertTrue(contains(ru, "a", 6, null));
                assertTrue(ru.size() == 1);
                cases.add(4);
                break;
            case "b = a;":
                assertTrue(contains(rd, "a", 5, null));
                assertTrue(rd.size() == 1);
                assertTrue(contains(ru, "b", 5, null));
                assertTrue(ru.size() == 1);
                break;
            case "a = 12;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "a = 14;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 4, null));
                assertTrue(contains(ru, "a", 10, null));
                assertTrue(ru.size() == 2);
                break;
            case "b = a + 1;":
                assertTrue(contains(rd, "a", 2, null));
                assertTrue(contains(rd, "a", 8, null));
                assertTrue(rd.size() == 2);
                assertTrue(ru.isEmpty());
                break;
            }
        }
    }
    
    public void testConditional() throws Exception {
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {        \n" //1
                + "    int a = 10;      \n" //2
                + "    int b = 20;      \n" //3
                + "    if(a == 10) {    \n" //4
                + "        a = b;       \n" //5
                + "    }                \n" //6
                + "    else {           \n" //7
                + "        b = a;       \n" //8
                + "        a = 12;      \n" //9
                + "        a = 14;      \n" //10
                + "    }                \n" //11
                + "    b = a + 1;       \n" //12
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "int a = 10;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 4, null));
                assertTrue(contains(ru, "a", 8, null));
                assertTrue(ru.size() == 2);
                break;
            case "int b = 20;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "b", 5, null));
                assertTrue(ru.size() == 1);
                break;
            case "a == 10":
                assertTrue(contains(rd, "a", 2, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
            case "a = b;":
                //this enters on both the statement and the binary expression it contains
                if (node instanceof IASTStatement) {
                    assertTrue(contains(rd, "b", 3, null));
                    assertTrue(rd.size() == 1);
                    assertTrue(contains(ru, "a", 12, null));
                    assertTrue(ru.size() == 1);
                }
                break;
            case "b = a;":
                assertTrue(contains(rd, "a", 2, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
                break;
            case "a = 12;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "a = 14;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 12, null));
                assertTrue(ru.size() == 1);
                break;
            case "b = a + 1;":
                assertTrue(contains(rd, "a", 5, null));
                assertTrue(contains(rd, "a", 10, null));
                assertTrue(rd.size() == 2);
                assertTrue(ru.isEmpty());
                break;
            }
        }
    }
    
    public void testDoWhile() throws Exception {
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {        \n" //1
                + "    int a = 10;      \n" //2
                + "    int b = 20;      \n" //3
                + "    do {             \n" //4
                + "        a = b;       \n" //5
                + "        b = a;       \n" //6
                + "        a = 12;      \n" //7
                + "        a = 14;      \n" //8
                + "    } while(a < 12); \n" //9
                + "    b = a + 1;       \n" //10
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "int a = 10;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "int b = 20;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "b", 5, null));
                assertTrue(ru.size() == 1);
                break;
            case "a = b;":
                assertTrue(contains(rd, "b", 3, null));
                assertTrue(contains(rd, "b", 6, null));
                assertTrue(rd.size() == 2);
                assertTrue(contains(ru, "a", 6, null));
                assertTrue(ru.size() == 1);
                break;
            case "b = a;":
                assertTrue(contains(rd, "a", 5, null));
                assertTrue(rd.size() == 1);
                assertTrue(contains(ru, "b", 5, null));
                assertTrue(ru.size() == 1);
                break;
            case "a = 12;":
                assertTrue(rd.isEmpty());
                assertTrue(ru.isEmpty());
                break;
            case "a = 14;":
                assertTrue(rd.isEmpty());
                assertTrue(contains(ru, "a", 9, null));
                assertTrue(contains(ru, "a", 10, null));
                assertTrue(ru.size() == 2);
                break;
            case "a < 12":
                assertTrue(contains(rd, "a", 8, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
                break;
            case "b = a + 1;":
                assertTrue(contains(rd, "a", 8, null));
                assertTrue(rd.size() == 1);
                assertTrue(ru.isEmpty());
                break;
            }
        }
    }
    
    public void testForLoopGlobals() throws Exception {
        IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "int x;"
                + "void main() {            \n" //1
                + "    int a, b;            \n" //2
                + "    for(int i = 0;       \n" //3
                + "       i < 10;           \n" //4
                + "       i++) {            \n" //5
                + "        b = x;           \n" //6
                + "        x = i;           \n" //7
                + "    }                    \n" //8
                + "    a = x;               \n" //9
                + "}");
        IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
        ReachingDefinitions rda = new ReachingDefinitions(func);
        for(IASTNode node : getStatements(func)) {
            Set<IASTName> rd = rda.reachingDefinitions(node);
            Set<IASTName> ru = rda.reachedUses(node);
            switch(node.getRawSignature()) {
            case "b = x;":
            	assertTrue(containsGlobal(rd, "x"));
            	assertTrue(contains(rd, "x", 7, null));
            	assertTrue(rd.size() == 2);
            	assertTrue(ru.isEmpty());
            	break;
            case "x = i;":
            	assertTrue(contains(rd, "i", 3, null));
            	assertTrue(contains(rd, "i", 5, null));
            	assertTrue(rd.size() == 2);
            	assertTrue(contains(ru, "x", 6, null));
            	assertTrue(contains(ru, "x", 9, null));
            	assertTrue(containsGlobal(ru, "x"));
            	assertTrue(ru.size() == 3);
            	break;
            case "a = x;":
            	assertTrue(containsGlobal(rd, "x"));
            	assertTrue(contains(rd, "x", 7, null));
            	assertTrue(rd.size() == 2);
            	assertTrue(ru.isEmpty());
            	break;
            }
        }
    }
    
    
    private List<IASTNode> getStatements(IASTFunctionDefinition func) {
        List<IASTNode> stmts = new ArrayList<IASTNode>();
        stmts.addAll(ASTUtil.find(func, IASTStatement.class));
        stmts.addAll(ASTUtil.find(func, IASTExpression.class));
        return stmts;
    }
    
    private boolean contains(Set<IASTName> names, String name, Integer lineNumber, Integer offset) {
        for(IASTName n : names) {
            if(checkName(n, name, lineNumber, offset)) 
                return true;
        }
        return false;
    }
    
    private boolean containsGlobal(Set<IASTName> names, String global) {
    	for(IASTName n : names) {
    		if(n instanceof Global && n.resolveBinding().getName().equals(global)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    private boolean checkName(IASTName occurrence, String name, Integer lineNumber, Integer offset) {
        boolean ok = true;
        if(name != null)
            ok &= occurrence.getRawSignature().equals(name);
        if(lineNumber != null)
            ok &= occurrence.getFileLocation().getStartingLineNumber() == lineNumber;
        if(offset != null)
            ok &= occurrence.getFileLocation().getNodeOffset() == offset;
        return ok;
    }
    
}
