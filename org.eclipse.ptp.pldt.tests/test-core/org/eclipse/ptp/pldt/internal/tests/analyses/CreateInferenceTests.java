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

import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CopyinInference;
import org.eclipse.ptp.pldt.openacc.core.dataflow.CreateInference;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

import junit.framework.TestCase;

public class CreateInferenceTests extends TestCase {

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
		Map<IASTStatement, Set<IBinding>> copyins = new CopyinInference(stmts).get();
		for (IASTStatement stmt : copyins.keySet()) {
			assertTrue(copyins.get(stmt).isEmpty());
		}
	}

	public void testVarsButNoCreate() throws CoreException {
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
		Map<IASTStatement, Set<IBinding>> copyins = new CopyinInference(stmts).get();
		for (IASTStatement stmt : copyins.keySet()) {
			assertTrue(copyins.get(stmt).isEmpty());
		}
	}

	public void testSimple() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString("" 
				+ "void main() {                        \n" // 1
				+ "    int a;                           \n" // 2
				+ "    int b;                           \n" // 3
				+ "	   #pragma acc data copyin(a, b)    \n" // 4
				+ "	   {                                \n" // 5
				+ "        #pragma acc parallel loop    \n" // 6
				+ "        for(int i = 0; i < 10; i++) {\n" // 7
				+ "             int tmp = a;            \n" // 8
				+ "             b = a;                  \n" // 9
				+ "             a = b;                  \n" // 10
				+ "             a = tmp;                \n" // 11
				+ "        }                            \n" // 12
				+ "    }                                \n" // 13
				+ "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> create = new CreateInference(stmts).get();
		for (IASTStatement stmt : create.keySet()) {
			if (stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(create, stmt, "a"));
				assertTrue(containsBinding(create, stmt, "b"));
				assertTrue(create.get(stmt).size() == 2);
			} else {
				switch (stmt.getFileLocation().getStartingLineNumber()) {
				case 7:
					assertTrue(create.get(stmt).isEmpty());
					break;
				default:
					// we should be covering every possible case
					assertTrue(false);
					break;
				}
			}
		}
	}

	public void testSimpleInitialized() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString("" 
				+ "void main() {                        \n" // 1
				+ "    int a = 9;                       \n" // 2
				+ "    int b = 8;                       \n" // 3
				+ "	   #pragma acc data copyin(a, b)    \n" // 4
				+ "	   {                                \n" // 5
				+ "        #pragma acc parallel loop    \n" // 6
				+ "        for(int i = 0; i < 10; i++) {\n" // 7
				+ "             int tmp = a;            \n" // 8
				+ "             b = a;                  \n" // 9
				+ "             a = b;                  \n" // 10
				+ "             a = tmp;                \n" // 11
				+ "        }                            \n" // 12
				+ "    }                                \n" // 13
				+ "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> create = new CreateInference(stmts).get();
		for (IASTStatement stmt : create.keySet()) {
			if (stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(create, stmt, "b"));
				assertTrue(create.get(stmt).size() == 1);
			} else {
				switch (stmt.getFileLocation().getStartingLineNumber()) {
				case 7:
					assertTrue(create.get(stmt).isEmpty());
					break;
				default:
					// we should be covering every possible case
					assertTrue(false);
					break;
				}
			}
		}
	}

	public void testSimpleArrays() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString("" 
				+ "void main() {                        \n" // 1
				+ "    int a[3];                        \n" // 2
				+ "    int b[3];                        \n" // 3
				+ "	   #pragma acc data copyin(a, b)    \n" // 4
				+ "	   {                                \n" // 5
				+ "        #pragma acc parallel loop    \n" // 6
				+ "        for(int i = 0; i < 10; i++) {\n" // 7
				+ "             int tmp = a[i];         \n" // 8
				+ "             b[i] = a[i];            \n" // 9
				+ "             a[i] = b[i];            \n" // 10
				+ "             a[i] = tmp;             \n" // 11
				+ "        }                            \n" // 12
				+ "    }                                \n" // 13
				+ "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> create = new CreateInference(stmts).get();
		for (IASTStatement stmt : create.keySet()) {
			if (stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(create, stmt, "a"));
				assertTrue(containsBinding(create, stmt, "b"));
				assertTrue(create.get(stmt).size() == 2);
			} else {
				switch (stmt.getFileLocation().getStartingLineNumber()) {
				case 7:
					assertTrue(create.get(stmt).isEmpty());
					break;
				default:
					// we should be covering every possible case
					assertTrue(false);
					break;
				}
			}
		}
	}

	public void testSimpleArraysInitialized() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString("" 
				+ "void main() {                        \n" // 1
				+ "    int a[3] = {1, 2, 3};            \n" // 2
				+ "    int b[3] = {4, 5, 6};            \n" // 3
				+ "	   #pragma acc data copyin(a, b)    \n" // 4
				+ "	   {                                \n" // 5
				+ "        #pragma acc parallel loop    \n" // 6
				+ "        for(int i = 0; i < 10; i++) {\n" // 7
				+ "             int tmp = a[i];         \n" // 8
				+ "             b[i] = a[i];            \n" // 9
				+ "             a[i] = b[i];            \n" // 10
				+ "             a[i] = tmp;             \n" // 11
				+ "        }                            \n" // 12
				+ "    }                                \n" // 13
				+ "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> create = new CreateInference(stmts).get();
		for (IASTStatement stmt : create.keySet()) {
			assertTrue(create.get(stmt).isEmpty());
		}
	}
	
	public void testAccessesInDataConstructNested() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                            \n" //1
                + "    int a[3];                            \n" //2
                + "    int b[3];                            \n" //3
                + "    int c[3];                            \n" //4
                + "	   #pragma acc data copyin(a, b)        \n" //5
                + "	   {                                    \n" //6
                + "        #pragma acc data copyin(c)       \n" //7
                + "        {                                \n" //8
                + "            #pragma acc parallel loop    \n" //9
                + "            for(int i = 0; i < 10; i++) {\n" //10
                + "                 c[i] = 10;              \n" //11
                + "                 a[i] = b[2];            \n" //12
                + "                 b[i] = a[2];            \n" //13
                + "                 b[i] = c[i];            \n" //14
                + "            }                            \n" //15
                + "        }                                \n" //16
                + "        for(int i = 0; i < 3; i++) {     \n" //17
                + "            c[i] = a[i];                 \n" //18
                + "        }                                \n" //19
                + "    }                                    \n" //20
                + "    for(int i = 0; i < 3; i++) {         \n" //21
                + "        c[i] = a[i];                     \n" //22
                + "    }                                    \n" //23
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> creates = new CreateInference(stmts).get(); 
		for(IASTStatement stmt : creates.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(creates, stmt, "b"));
				assertTrue(containsBinding(creates, stmt, "c"));
				assertTrue(creates.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(creates.get(stmt).isEmpty());
						break;
					case 10:
						assertTrue(creates.get(stmt).isEmpty());
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}

	public void testSimpleMultipleNested() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                            \n" //1
                + "    int a[3];                            \n" //2
                + "    int b[3];                            \n" //3
                + "    int c[3];                            \n" //4
                + "	   #pragma acc data copyin(a, b)        \n" //5
                + "	   {                                    \n" //6
                + "        #pragma acc parallel loop        \n" //7
                + "        for(int i = 0; i < 10; i++) {    \n" //8 
                + "             a[i] = c[2];                \n" //9
                + "             c[i] = a[2];                \n" //10
                + "        }                                \n" //11
                + "                                         \n" //12
                + "        int d[3];                        \n" //13
                + "                                         \n" //14
                + "        #pragma acc parallel loop        \n" //15
                + "        for(int i = 0; i < 10; i++) {    \n" //16
                + "             c[i] = d[2];                \n" //17
                + "             d[i] = c[2];                \n" //18
                + "        }                                \n" //19
                + "    }                                    \n" //20
                + "    for(int i = 0; i < 3; i++) {         \n" //21
                + "        c[i] = b[i];                     \n" //22
                + "    }                                    \n" //23
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> creates = new CreateInference(stmts).get(); 
		for(IASTStatement stmt : creates.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(creates, stmt, "a"));
				assertTrue(creates.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(creates.get(stmt).isEmpty());
						break;
					case 16:
						assertTrue(containsBinding(creates, stmt, "d"));
						assertTrue(creates.get(stmt).size() == 1);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	public void testKernelsLoop() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                            \n" //1
                + "    int a[3];                            \n" //2
                + "    int b[3];                            \n" //3
                + "    int c[3];                            \n" //4
                + "	   #pragma acc data copyin(a, b)        \n" //5
                + "	   {                                    \n" //6
                + "        #pragma acc kernels loop         \n" //7
                + "        for(int i = 0; i < 10; i++) {    \n" //8 
                + "             a[i] = c[2];                \n" //9
                + "             c[i] = a[2];                \n" //10
                + "        }                                \n" //11
                + "                                         \n" //12
                + "        int d[3];                        \n" //13
                + "                                         \n" //14
                + "        #pragma acc kernels loop         \n" //15
                + "        for(int i = 0; i < 10; i++) {    \n" //16
                + "             c[i] = d[2];                \n" //17
                + "             d[i] = c[2];                \n" //18
                + "        }                                \n" //19
                + "    }                                    \n" //20
                + "    for(int i = 0; i < 3; i++) {         \n" //21
                + "        c[i] = b[i];                     \n" //22
                + "    }                                    \n" //23
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> creates = new CreateInference(stmts).get(); 
		for(IASTStatement stmt : creates.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(creates, stmt, "a"));
				assertTrue(creates.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(creates.get(stmt).isEmpty());
						break;
					case 16:
						assertTrue(containsBinding(creates, stmt, "d"));
						assertTrue(creates.get(stmt).size() == 1);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	public void testParallel() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                                \n" //1
                + "    int a[3];                                \n" //2
                + "    int b[3];                                \n" //3
                + "    int c[3];                                \n" //4
                + "	   #pragma acc data copyin(a, b)            \n" //5
                + "	   {                                        \n" //6
                + "        #pragma acc parallel                 \n" //7
                + "        {                                    \n" //8
                + "            #pragma acc loop                 \n" //9
                + "            for(int i = 0; i < 10; i++) {    \n" //10 
                + "                a[i] = c[2];                 \n" //11
                + "                c[i] = a[2];                 \n" //12
                + "            }                                \n" //13
                + "        }                                    \n" //14
                + "                                             \n" //15
                + "        int d[3];                            \n" //16
                + "                                             \n" //17
                + "        #pragma acc parallel                 \n" //18
                + "        {                                    \n" //19
                + "            #pragma acc loop                 \n" //20
                + "            for(int i = 0; i < 10; i++) {    \n" //21
                + "                c[i] = d[2];                 \n" //22
                + "                d[i] = c[2];                 \n" //23
                + "            }                                \n" //24
                + "        }                                    \n" //25
                + "    }                                        \n" //26
                + "    for(int i = 0; i < 3; i++) {             \n" //27
                + "        c[i] = b[i];                         \n" //28
                + "    }                                        \n" //29
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> creates = new CreateInference(stmts).get(); 
		for(IASTStatement stmt : creates.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(creates, stmt, "a"));
				assertTrue(creates.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(creates.get(stmt).isEmpty());
						break;
					case 19:
						assertTrue(containsBinding(creates, stmt, "d"));
						assertTrue(creates.get(stmt).size() == 1);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	public void testKernels() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                                \n" //1
                + "    int a[3];                                \n" //2
                + "    int b[3];                                \n" //3
                + "    int c[3];                                \n" //4
                + "	   #pragma acc data copyin(a, b)            \n" //5
                + "	   {                                        \n" //6
                + "        #pragma acc kernels                  \n" //7
                + "        {                                    \n" //8
                + "            #pragma acc loop                 \n" //9
                + "            for(int i = 0; i < 10; i++) {    \n" //10 
                + "                a[i] = c[2];                 \n" //11
                + "                c[i] = a[2];                 \n" //12
                + "            }                                \n" //13
                + "        }                                    \n" //14
                + "                                             \n" //15
                + "        int d[3];                            \n" //16
                + "                                             \n" //17
                + "        #pragma acc kernels                  \n" //18
                + "        {                                    \n" //19
                + "            #pragma acc loop                 \n" //20
                + "            for(int i = 0; i < 10; i++) {    \n" //21
                + "                c[i] = d[2];                 \n" //22
                + "                d[i] = c[2];                 \n" //23
                + "            }                                \n" //24
                + "        }                                    \n" //25
                + "    }                                        \n" //26
                + "    for(int i = 0; i < 3; i++) {             \n" //27
                + "        c[i] = b[i];                         \n" //28
                + "    }                                        \n" //29
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> creates = new CreateInference(stmts).get(); 
		for(IASTStatement stmt : creates.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(creates, stmt, "a"));
				assertTrue(creates.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(creates.get(stmt).isEmpty());
						break;
					case 19:
						assertTrue(containsBinding(creates, stmt, "d"));
						assertTrue(creates.get(stmt).size() == 1);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	public void testComplexNest() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                                  \n" //1
                + "    int a[3];                                  \n" //2
                + "    int b[3];                                  \n" //3
                + "    int c[3] = {1, 2, 3};                      \n" //4
                + "    #pragma acc data                           \n" //5
                + "    {                                          \n" //6
                + "        #pragma acc data                       \n" //7
                + "        {                                      \n" //8
                + "            #pragma acc parallel loop          \n" //9
                + "            for(int i = 0; i < 3; i++) {       \n" //10
                + "                a[i] = c[i];                   \n" //11
                + "                b[i] = a[i];                   \n" //12
                + "            }                                  \n" //13
                + "        }                                      \n" //14
                + "        b = c;                                 \n" //15
                + "        int d[3];                              \n" //16
                + "        #pragma acc parallel loop              \n" //17
                + "        for(int i = 0; i < 3; i++) {           \n" //18
                + "            a[i] = c[i + 1];                   \n" //19
                + "            d[i] = b[i] + d[i];                \n" //20
                + "        }                                      \n" //21
                + "        d = c;                                 \n" //22
                + "        for(int i = 0; i < 3; i++) {           \n" //23
                + "            d[i] = c[i];                       \n" //24
                + "        }                                      \n" //25
                + "    }                                          \n" //26
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CreateInference infer = new CreateInference(stmts);
		Map<IASTStatement, Set<IBinding>> creates = infer.get(); 
		for(IASTStatement stmt : creates.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(creates, stmt, "a"));
				assertTrue(creates.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(containsBinding(creates, stmt, "b"));
						assertTrue(creates.get(stmt).size() == 1);
						break;
					case 10:
						assertTrue(creates.get(stmt).isEmpty());
						break;
					case 18:
						assertTrue(containsBinding(creates, stmt, "d"));
						assertTrue(creates.get(stmt).size() == 1);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	public void testVariousNameUses() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "#include <stdio.h>                            \n" //1
                + "#include <stdlib.h>                           \n" //2
                + "typedef int integer;                          \n" //3
                + "typedef struct { int x, y; } point_t;         \n" //4
                + "int main(void) {                              \n" //5
                + "    int a[10];                                \n" //6
                + "    {                                         \n" //7
                + "        #pragma acc parallel loop             \n" //8
                + "        for (int i = 0; i < 10; i++) {        \n" //9
                + "            point_t pt;                       \n" //10
                + "done:                                         \n"
                + "            a[i] = pt.x = pt.y = (integer)sqrt(1.0);\n"
                + "        }                                     \n"
                + "    }                                         \n"
                + "    return EXIT_SUCCESS;\n"
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		CreateInference infer = new CreateInference(stmts);
		Map<IASTStatement, Set<IBinding>> creates = infer.get(); 
		for(IASTStatement stmt : creates.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(creates, stmt, "a"));
				assertTrue(creates.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 9:
						assertTrue(creates.get(stmt).isEmpty());
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	private boolean containsBinding(Map<IASTStatement, Set<IBinding>> creates, IASTStatement statement,
			String binding) {
		Set<IBinding> bindings = creates.get(statement);
		for (IBinding b : bindings) {
			if (b.getName().equals(binding)) {
				return true;
			}
		}
		return false;
	}

	private IASTCompoundStatement getFirstChildCompound(IASTStatement statement) {
		if (!(statement instanceof IASTCompoundStatement)) {
			throw new IllegalArgumentException();
		}

		for (IASTStatement s : ((IASTCompoundStatement) statement).getStatements()) {
			if (s instanceof IASTCompoundStatement) {
				return (IASTCompoundStatement) s;
			}
		}
		return null;
	}

}
