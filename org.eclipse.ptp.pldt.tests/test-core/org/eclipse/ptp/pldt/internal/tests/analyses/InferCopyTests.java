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

import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyinInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyoutInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.DataTransferInference;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

import junit.framework.TestCase;

public class InferCopyTests extends TestCase {
	
	public void testNothing() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                    \n"
                + "    #pragma acc parallel loop    \n"
                + "    for(int i = 0; i < 10; i++) {\n"
                + "        ;//do nothing            \n"
                + "    }                            \n"
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTStatement[] stmts = ((IASTCompoundStatement) func.getBody()).getStatements();
		CopyinInference copyins = new CopyinInference(stmts);
		CopyoutInference copyouts = new CopyoutInference(stmts);
		CopyInference copies = new CopyInference(copyins, copyouts);
		for(IASTStatement stmt : copyouts.get().keySet()) {
			assertTrue(copyouts.get().get(stmt).isEmpty());
		}
		for(IASTStatement stmt : copyins.get().keySet()) {
			assertTrue(copyins.get().get(stmt).isEmpty());
		}
		for(IASTStatement stmt : copies.get().keySet()) {
			assertTrue(copies.get().get(stmt).isEmpty());
		}
	}
	
	public void testVarsButNoCopyout() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                        \n"
                + "    int a, b;                        \n"
                + "	   #pragma acc data copyin(a, b)    \n"
                + "	   {                                \n"
                + "        #pragma acc parallel loop    \n"
                + "        for(int i = 0; i < 10; i++) {\n"
                + "            ;//do nothing            \n"
                + "        }                            \n"
                + "    }                                \n"
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CopyinInference copyins = new CopyinInference(stmts);
		CopyoutInference copyouts = new CopyoutInference(stmts);
		CopyInference copies = new CopyInference(copyins, copyouts);
		for(IASTStatement stmt : copies.get().keySet()) {
			assertTrue(copyins.get().get(stmt).isEmpty());
			assertTrue(copyouts.get().get(stmt).isEmpty());
			assertTrue(copies.get().get(stmt).isEmpty());
		}
	}
	
	public void testSimple() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                        \n" //1
                + "    int a;                           \n" //2
                + "    int b = 6;                       \n" //3
                + "	   #pragma acc data copyin(a, b)    \n" //4
                + "	   {                                \n" //5
                + "        #pragma acc parallel loop    \n" //6
                + "        for(int i = 0; i < 10; i++) {\n" //7
                + "             int tmp = a;            \n" //8
                + "             a = b;                  \n" //9
                + "             b = tmp;                \n" //10
                + "        }                            \n" //11
                + "    }                                \n" //12
                + "    int c = a + b;                   \n" //13
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CopyinInference copyins = new CopyinInference(stmts);
		CopyoutInference copyouts = new CopyoutInference(stmts);
		CopyInference copies = new CopyInference(copyins, copyouts);
		for(IASTStatement stmt : copies.get().keySet()) {
			if(stmt.equals(copies.getRoot())) {
				assertTrue(copyins.get().get(stmt).isEmpty());
				assertTrue(containsBinding(copyouts, stmt, "a"));
				assertTrue(copyouts.get().get(stmt).size() == 1);
				assertTrue(containsBinding(copies, stmt, "b"));
				assertTrue(copies.get().get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
				case 7:
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(copyouts.get().get(stmt).isEmpty());
					assertTrue(copies.get().get(stmt).isEmpty());
					break;
				default:
					assertTrue(false);
					break;
				}
			}
		}
	}

	public void testSimpleArrays() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                        \n" //1
                + "    int a[3] = {1, 2, 3};            \n" //2
                + "    int b[3] = {4, 5, 6};            \n" //3
                + "	   #pragma acc data copyin(a, b)    \n" //4
                + "	   {                                \n" //5
                + "        #pragma acc parallel loop    \n" //6
                + "        for(int i = 0; i < 10; i++) {\n" //7
                + "             a[i] = b[2];            \n" //8
                + "             b[i] = a[2];            \n" //9
                + "        }                            \n" //10
                + "    }                                \n" //11
                + "    for(int i = 0; i < 3; i++) {     \n" //12
                + "        a[i] = b[i];                 \n" //13
                + "    }                                \n" //14
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CopyinInference copyins = new CopyinInference(stmts);
		CopyoutInference copyouts = new CopyoutInference(stmts);
		CopyInference copies = new CopyInference(copyins, copyouts);
		for(IASTStatement stmt : copies.get().keySet()) {
			if(stmt.equals(copies.getRoot())) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(copyins.get().get(stmt).size() == 1);
				assertTrue(copyouts.get().get(stmt).isEmpty());
				assertTrue(containsBinding(copies, stmt, "b"));
				assertTrue(copies.get().get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
				case 7:
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(copyouts.get().get(stmt).isEmpty());
					assertTrue(copies.get().get(stmt).isEmpty());
					break;
				default:
					assertTrue(false);
					break;
				}
			}
		}
	}

	public void testDefsInDataConstruct() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                        \n" //1
                + "    int a[3] = {1, 2, 3};            \n" //2
                + "    int b[3] = {4, 5, 6};            \n" //3
                + "    int c[3] = {7, 8, 9};            \n" //4
                + "	   #pragma acc data copyin(a, b)    \n" //5
                + "	   {                                \n" //6
                + "        for(int i = 0; i < 3; i++) { \n" //7
                + "            a[i] = i;                \n" //8
                + "        }                            \n" //9
                + "        #pragma acc parallel loop    \n" //10
                + "        for(int i = 0; i < 10; i++) {\n" //11
                + "             a[i] = c[2];            \n" //12
                + "             b[i] = a[2];            \n" //13
                + "        }                            \n" //14
                + "        for(int i = 0; i < 3; i++) { \n" //15
                + "            c[i] = a[i];             \n" //16
                + "        }                            \n" //17
                + "    }                                \n" //18
                + "    for(int i = 0; i < 3; i++) {     \n" //19
                + "        c[i] = b[i];                 \n" //20
                + "    }                                \n" //21
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CopyinInference copyins = new CopyinInference(stmts);
		CopyoutInference copyouts = new CopyoutInference(stmts);
		CopyInference copies = new CopyInference(copyins, copyouts);
		for(IASTStatement stmt : copies.get().keySet()) {
			if(stmt.equals(copies.getRoot())) { //copyout b, in b
				assertTrue(containsBinding(copyins, stmt, "c"));
				assertTrue(copyins.get().get(stmt).size() == 1);
				assertTrue(containsBinding(copyouts, stmt, "b"));
				assertTrue(copyouts.get().get(stmt).size() == 1);
				assertTrue(copies.get().get(stmt).isEmpty());
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
				case 11: 
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(copyouts.get().get(stmt).isEmpty());
					assertTrue(containsBinding(copies, stmt, "a"));
					assertTrue(copies.get().get(stmt).size() == 1);
					break;
				default:
					assertTrue(false);
					break;
				}
			}
		}
	}

	public void testDefsInDataConstructNested() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                            \n" //1
                + "    int a[3] = {1, 2, 3};                \n" //2
                + "    int b[3] = {4, 5, 6};                \n" //3
                + "    int c[3] = {7, 8, 9};                \n" //4
                + "	   #pragma acc data copyin(a, b)        \n" //5
                + "	   {                                    \n" //6
                + "        for(int i = 0; i < 3; i++) {     \n" //7
                + "            b[i] = a[i];                 \n" //8
                + "        }                                \n" //9
                + "        #pragma acc data copyin(c)       \n" //10
                + "        {                                \n" //11
                + "            for(int i = 0; i < 3; i++) { \n" //12
                + "                a[i] = c[i];             \n" //13
                + "            }                            \n" //14
                + "            #pragma acc parallel loop    \n" //15
                + "            for(int i = 0; i < 10; i++) {\n" //16
                + "                a[i] = b[2];             \n" //17
                + "                b[i] = a[2];             \n" //18
                + "                c[i] = c[i] + 1;         \n" //19
                + "            }                            \n" //20
                + "            for(int i = 0; i < 3; i++) { \n" //21
                + "                c[i] = a[i];             \n" //22
                + "            }                            \n" //23
                + "        }                                \n" //24
                + "        for(int i = 0; i < 3; i++) {     \n" //25
                + "            c[i] = b[i];                 \n" //26
                + "        }                                \n" //27
                + "    }                                    \n" //28
                + "    for(int i = 0; i < 3; i++) {         \n" //29
                + "        b[i] = c[i];                     \n" //30
                + "    }                                    \n" //31
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CopyinInference copyins = new CopyinInference(stmts);
		CopyoutInference copyouts = new CopyoutInference(stmts);
		CopyInference copies = new CopyInference(copyins, copyouts);
		for(IASTStatement stmt : copies.get().keySet()) {
			if(stmt.equals(copies.getRoot())) { //copyout b, in b
				assertTrue(copyins.get().get(stmt).isEmpty());
				assertTrue(copyouts.get().get(stmt).isEmpty());
				assertTrue(containsBinding(copies, stmt, "c"));
				assertTrue(copies.get().get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
				case 11: 
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(copyouts.get().get(stmt).isEmpty());
					assertTrue(containsBinding(copies, stmt, "b"));
					assertTrue(copies.get().get(stmt).size() == 1);
					break;
				case 16:
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(copyouts.get().get(stmt).isEmpty());
					assertTrue(containsBinding(copies, stmt, "a"));
					assertTrue(copies.get().get(stmt).size() == 1);
					break;
				default:
					assertTrue(false);
					break;
				}
			}
		}
	}

	public void testSimpleMultipleNested() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                            \n" //1
                + "    int a[3] = {1, 2, 3};                \n" //2
                + "    int b[3] = {4, 5, 6};                \n" //3
                + "    int c[3] = {-1, -1, -1};             \n" //4
                + "	   #pragma acc data copyin(a, b)        \n" //5
                + "	   {                                    \n" //6
                + "        #pragma acc parallel loop        \n" //7
                + "        for(int i = 0; i < 10; i++) {    \n" //8
                + "             a[i] = c[2];                \n" //9
                + "             c[i] = a[2];                \n" //10
                + "        }                                \n" //11
                + "                                         \n" //12
                + "        int d[3] = {10, 11, 12};         \n" //13
                + "        for(int i = 0; i < 3; i++) {     \n" //14
                + "            d[i] = c[i];                 \n" //15
                + "        }                                \n" //16
                + "                                         \n" //17
                + "        #pragma acc parallel loop        \n" //18
                + "        for(int i = 0; i < 10; i++) {    \n" //19
                + "             d[i] = b[2];                \n" //20
                + "             b[i] = d[2];                \n" //21
                + "        }                                \n" //22
                + "        for(int i = 0; i < 3; i++) {     \n" //23
                + "            c[i] = d[i];                 \n" //24
                + "        }                                \n" //25
                + "    }                                    \n" //26
                + "    for(int i = 0; i < 3; i++) {         \n" //27
                + "        c[i] = b[i];                     \n" //28
                + "        c[i] = a[i];                     \n" //29
                + "    }                                    \n" //30
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CopyinInference copyins = new CopyinInference(stmts);
		CopyoutInference copyouts = new CopyoutInference(stmts);
		CopyInference copies = new CopyInference(copyins, copyouts);
		for(IASTStatement stmt : copies.get().keySet()) {
			if(stmt.equals(copies.getRoot())) { //copyout b, in b
				assertTrue(containsBinding(copyins, stmt, "c"));
				assertTrue(copyins.get().get(stmt).size() == 1);
				assertTrue(copyouts.get().get(stmt).isEmpty());
				assertTrue(containsBinding(copies, stmt, "a"));
				assertTrue(containsBinding(copies, stmt, "b"));
				assertTrue(copies.get().get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
				case 8: 
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(containsBinding(copyouts, stmt, "c"));
					assertTrue(copyouts.get().get(stmt).size() == 1);
					assertTrue(copies.get().get(stmt).isEmpty());
					break;
				case 19:
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(copyouts.get().get(stmt).isEmpty());
					assertTrue(containsBinding(copies, stmt, "d"));
					assertTrue(copies.get().get(stmt).size() == 1);
					break;
				default:
					assertTrue(false);
					break;
				}
			}
		}
	}
	
	public void testComplexNest() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                                  \n" //1
                + "    int a[3] = {1, 2, 3};                      \n" //2
                + "    int b[3] = {4, 5, 6};                      \n" //3
                + "    int c[3] = {-1, -1, -1};                   \n" //4
                + "    #pragma acc data                           \n" //5
                + "    {                                          \n" //6
                + "        #pragma acc data                       \n" //7
                + "        {                                      \n" //8
                + "            #pragma acc parallel loop          \n" //9
                + "            {                                  \n" //10
                + "                for(int i = 0; i < 3; i++) {   \n" //11
                + "                    a[i] = b[0];               \n" //12
                + "                    b[i] = c[1];               \n" //13
                + "                    c[i] = a[2];               \n" //14
                + "                }                              \n" //15
                + "            }                                  \n" //16
                + "            for(int i = 0; i < 3; i++) {       \n" //17
                + "                printf(\"%d\", b[i]);          \n" //18
                + "            }                                  \n" //19
                + "        }                                      \n" //20
                + "        int d[3] = {-1, -2, -3};               \n" //21
                + "        #pragma acc parallel loop              \n" //22
                + "        for(int i = 0; i < 3; i++) {           \n" //23
                + "            a[i] = b[0];                       \n" //24
                + "            b[i] = c[1];                       \n" //25
                + "            c[i] = d[2];                       \n" //26
                + "            d[i] = a[0];                       \n" //27
                + "        }                                      \n" //28
                + "        for(int i = 0; i < 3; i++) {           \n" //29
                + "            printf(\"%d\", d[i]);              \n" //30
                + "        }                                      \n" //31
                + "    }                                          \n" //32
                + "    for(int i = 0; i < 3; i++) {               \n" //33
                + "        printf(\"%d\", a[i]);                  \n" //34
                + "        printf(\"%d\", b[i]);                  \n" //35
                + "        printf(\"%d\", c[i]);                  \n" //36
                + "    }                                          \n" //37
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CopyinInference copyins = new CopyinInference(stmts);
		CopyoutInference copyouts = new CopyoutInference(stmts);
		CopyInference copies = new CopyInference(copyins, copyouts);
		for(IASTStatement stmt : copies.get().keySet()) {
			if(stmt.equals(copies.getRoot())) {
				assertTrue(copyins.get().get(stmt).isEmpty());
				assertTrue(copyouts.get().get(stmt).isEmpty());
				assertTrue(containsBinding(copies, stmt, "a"));
				assertTrue(containsBinding(copies, stmt, "b"));
				assertTrue(containsBinding(copies, stmt, "c"));
				assertTrue(copies.get().get(stmt).size() == 3);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
				case 8: 
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(containsBinding(copyouts, stmt, "a"));
					assertTrue(containsBinding(copyouts, stmt, "c"));
					assertTrue(copyouts.get().get(stmt).size() == 2);
					assertTrue(copies.get().get(stmt).isEmpty());
					break;
				case 10:
					assertTrue(copyins.get().get(stmt).isEmpty());
					assertTrue(containsBinding(copyouts, stmt, "b"));
					assertTrue(copyouts.get().get(stmt).size() == 1);
					assertTrue(copies.get().get(stmt).isEmpty());
					break;
				case 23:
					assertTrue(containsBinding(copyins, stmt, "a"));
					assertTrue(containsBinding(copyins, stmt, "b"));
					assertTrue(containsBinding(copyins, stmt, "c"));
					assertTrue(copyins.get().get(stmt).size() == 3);
					assertTrue(copyouts.get().get(stmt).isEmpty());
					assertTrue(containsBinding(copies, stmt, "d"));
					assertTrue(copies.get().get(stmt).size() == 1);
					break;
				default:
					assertTrue(false);
					break;
				}
			}
		}
	}
	
	private boolean containsBinding(DataTransferInference copyouts, IASTStatement statement, String binding) {
		Set<IBinding> bindings = copyouts.get().get(statement);
		for(IBinding b : bindings) {
			if(b.getName().equals(binding)) {
				return true;
			}
		}
		return false;
	}
	
	private IASTCompoundStatement getFirstChildCompound(IASTStatement statement) {
		if(!(statement instanceof IASTCompoundStatement)) {
			throw new IllegalArgumentException();
		}
		
		for(IASTStatement s : ((IASTCompoundStatement) statement).getStatements()) {
			if(s instanceof IASTCompoundStatement) {
				return (IASTCompoundStatement) s;
			}
		}
		return null;
	}
	
	
	
}
