package org.eclipse.ptp.pldt.internal.tests.analyses;

import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCopyin;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

import junit.framework.TestCase;

public class InferCopyinTests extends TestCase {

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
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			assertTrue(copyins.get(stmt).isEmpty());
		}
	}
	
	public void testVarsButNoCopyin() throws CoreException {
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
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			assertTrue(copyins.get(stmt).isEmpty());
		}
	}
	
	public void testSimple() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                        \n" //1
                + "    int a = 10;                      \n" //2
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
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(containsBinding(copyins, stmt, "b"));
				assertTrue(copyins.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 7:
						assertTrue(copyins.get(stmt).isEmpty());
						break;
					default:
						//we should be covering every possible case
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
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(containsBinding(copyins, stmt, "b"));
				assertTrue(copyins.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 7:
						assertTrue(copyins.get(stmt).isEmpty());
						break;
					default:
						//we should be covering every possible case
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
                + "	   #pragma acc data copyin(a, b)    \n" //4
                + "	   {                                \n" //5
                + "        int c[3] = {7, 8, 9};        \n" //6
                + "        #pragma acc parallel loop    \n" //7
                + "        for(int i = 0; i < 10; i++) {\n" //8
                + "             a[i] = c[2];            \n" //9
                + "             c[i] = a[2];            \n" //10
                + "        }                            \n" //11
                + "    }                                \n" //12
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(copyins.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(containsBinding(copyins, stmt, "c"));
						assertTrue(copyins.get(stmt).size() == 1);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	public void testSimpleNestedDataConstruct() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                            \n" //1
                + "    int a[3] = {1, 2, 3};                \n" //2
                + "    int b[3] = {4, 5, 6};                \n" //3
                + "	   #pragma acc data copyin(a, b)        \n" //4
                + "	   {                                    \n" //5
                + "        int c[3] = {7, 8, 9};            \n" //6
                + "        #pragma acc data copyin(c)       \n" //7
                + "        {                                \n" //8
                + "            #pragma acc parallel loop    \n" //9
                + "            for(int i = 0; i < 10; i++) {\n" //10
                + "                 a[i] = b[2];            \n" //11
                + "                 b[i] = a[2];            \n" //12
                + "            }                            \n" //13
                + "        }                                \n" //14
                + "        int d[3] = {10, 11, 12};         \n" //15
                + "    }                                    \n" //16
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(containsBinding(copyins, stmt, "b"));
				assertTrue(copyins.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(copyins.get(stmt).isEmpty());
						break;
					case 10:
						assertTrue(copyins.get(stmt).isEmpty());
						break;
					default:
						//we should be covering every possible case
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
                + "	   #pragma acc data copyin(a, b)        \n" //4
                + "	   {                                    \n" //5
                + "        int c[3] = {7, 8, 9};            \n" //6
                + "        #pragma acc data copyin(c)       \n" //7
                + "        {                                \n" //8
                + "            #pragma acc parallel loop    \n" //9
                + "            for(int i = 0; i < 10; i++) {\n" //10
                + "                 a[i] = c[2];            \n" //11
                + "                 c[i] = a[2];            \n" //12
                + "            }                            \n" //13
                + "        }                                \n" //14
                + "        int d[3] = {10, 11, 12};         \n" //15
                + "    }                                    \n" //16
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(copyins.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 8:
						assertTrue(containsBinding(copyins, stmt, "c"));
						assertTrue(copyins.get(stmt).size() == 1);
						break;
					case 10:
						assertTrue(copyins.get(stmt).isEmpty());
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
                + "    int a[3] = {1, 2, 3};                \n" //2
                + "    int b[3] = {4, 5, 6};                \n" //3
                + "    int c[3] = {-1, -1, -1};             \n" //4
                + "	   #pragma acc data copyin(a, b)        \n" //5
                + "	   {                                    \n" //6
                + "        c[0] = 7; c[1] = 8; c[2] = 9;    \n" //7
                + "        #pragma acc parallel loop        \n" //8
                + "        for(int i = 0; i < 10; i++) {    \n" //9
                + "             a[i] = c[2];                \n" //10
                + "             c[i] = a[2];                \n" //11
                + "        }                                \n" //12
                + "                                         \n" //13
                + "        int d[3] = {10, 11, 12};         \n" //14
                + "                                         \n" //15
                + "        #pragma acc parallel loop        \n" //16
                + "        for(int i = 0; i < 10; i++) {    \n" //17
                + "             d[i] = b[2];                \n" //18
                + "             b[i] = d[2];                \n" //19
                + "        }                                \n" //20
                + "    }                                    \n" //21
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(containsBinding(copyins, stmt, "b"));
				assertTrue(copyins.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 9:
						assertTrue(containsBinding(copyins, stmt, "c"));
						assertTrue(copyins.get(stmt).size() == 1);
						break;
					case 17:
						assertTrue(containsBinding(copyins, stmt, "d"));
						assertTrue(copyins.get(stmt).size() == 1);
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
                + "    int a[3] = {1, 2, 3};                \n" //2
                + "    int b[3] = {4, 5, 6};                \n" //3
                + "    int c[3] = {-1, -1, -1};             \n" //4
                + "	   #pragma acc data copyin(a, b)        \n" //5
                + "	   {                                    \n" //6
                + "        c[0] = 7; c[1] = 8; c[2] = 9;    \n" //7
                + "        #pragma acc kernels loop         \n" //8
                + "        for(int i = 0; i < 10; i++) {    \n" //9
                + "             a[i] = c[2];                \n" //10
                + "             c[i] = a[2];                \n" //11
                + "        }                                \n" //12
                + "                                         \n" //13
                + "        int d[3] = {10, 11, 12};         \n" //14
                + "                                         \n" //15
                + "        #pragma acc kernels loop         \n" //16
                + "        for(int i = 0; i < 10; i++) {    \n" //17
                + "             d[i] = b[2];                \n" //18
                + "             b[i] = d[2];                \n" //19
                + "        }                                \n" //20
                + "    }                                    \n" //21
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(containsBinding(copyins, stmt, "b"));
				assertTrue(copyins.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 9:
						assertTrue(containsBinding(copyins, stmt, "c"));
						assertTrue(copyins.get(stmt).size() == 1);
						break;
					case 17:
						assertTrue(containsBinding(copyins, stmt, "d"));
						assertTrue(copyins.get(stmt).size() == 1);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	public void testKernelsRegion() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                                \n" //1
                + "    int a[3] = {1, 2, 3};                    \n" //2
                + "    int b[3] = {4, 5, 6};                    \n" //3
                + "    int c[3] = {-1, -1, -1};                 \n" //4
                + "	   #pragma acc data copyin(a, b)            \n" //5
                + "	   {                                        \n" //6
                + "        c[0] = 7; c[1] = 8; c[2] = 9;        \n" //7
                + "        #pragma acc kernels                  \n" //8
                + "        {                                    \n" //9
                + "            #pragma acc loop                 \n" //10
                + "            for(int i = 0; i < 10; i++) {    \n" //11
                + "                 a[i] = c[2];                \n" //12
                + "                 c[i] = a[2];                \n" //13
                + "            }                                \n" //14
                + "                                             \n" //15
                + "            int d[3] = {10, 11, 12};         \n" //16
                + "                                             \n" //17
                + "            #pragma acc loop                 \n" //18
                + "            for(int i = 0; i < 10; i++) {    \n" //19
                + "                 d[i] = b[2];                \n" //20
                + "                 b[i] = d[2];                \n" //21
                + "            }                                \n" //22
                + "        }                                    \n" //23
                + "    }                                        \n" //24
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		InferCopyin infer = new InferCopyin(stmts);
		Map<IASTStatement, Set<IBinding>> copyins = infer.get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(containsBinding(copyins, stmt, "b"));
				assertTrue(copyins.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 9:
						assertTrue(containsBinding(copyins, stmt, "c"));
						assertTrue(copyins.get(stmt).size() == 1);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	public void testParallelRegion() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                                \n" //1
                + "    int a[3] = {1, 2, 3};                    \n" //2
                + "    int b[3] = {4, 5, 6};                    \n" //3
                + "    int c[3] = {-1, -1, -1};                 \n" //4
                + "	   #pragma acc data copyin(a, b)            \n" //5
                + "	   {                                        \n" //6
                + "        c[0] = 7; c[1] = 8; c[2] = 9;        \n" //7
                + "        #pragma acc parallel                 \n" //8
                + "        {                                    \n" //9
                + "            #pragma acc loop                 \n" //10
                + "            for(int i = 0; i < 10; i++) {    \n" //11
                + "                 a[i] = c[2];                \n" //12
                + "                 c[i] = a[2];                \n" //13
                + "            }                                \n" //14
                + "                                             \n" //15
                + "            int d[3] = {10, 11, 12};         \n" //16
                + "                                             \n" //17
                + "            #pragma acc loop                 \n" //18
                + "            for(int i = 0; i < 10; i++) {    \n" //19
                + "                 d[i] = b[2];                \n" //20
                + "                 b[i] = d[2];                \n" //21
                + "            }                                \n" //22
                + "        }                                    \n" //23
                + "    }                                        \n" //24
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(containsBinding(copyins, stmt, "b"));
				assertTrue(copyins.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 9:
						assertTrue(containsBinding(copyins, stmt, "c"));
						assertTrue(copyins.get(stmt).size() == 1);
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
                + "void main() {                            \n" //1
                + "    int a[3] = {1, 2, 3};                \n" //2
                + "    int b[3] = {4, 5, 6};                \n" //3
                + "    int c[3] = {-1, -1, -1};             \n" //4
                + "	   #pragma acc data copyin(a, b)        \n" //5
                + "	   {                                    \n" //6
                + "        c[0] = 7; c[1] = 8; c[2] = 9;    \n" //7
                + "        #pragma acc data copyin(c)       \n" //8
                + "        {                                \n" //9
                + "            b[0] = 0; b[1] = 2; b[2] = 4;\n" //10
                + "            #pragma acc parallel loop    \n" //11
                + "            for(int i = 0; i < 10; i++) {\n" //12
                + "                 a[i] = c[2];            \n" //13
                + "                 c[i] = a[2];            \n" //14
                + "                 b[i] = b[i] + 1;        \n" //15
                + "            }                            \n" //16
                + "        }                                \n" //17
                + "                                         \n" //18
                + "        int d[3] = {10, 11, 12};         \n" //19
                + "                                         \n" //20
                + "        #pragma acc parallel loop        \n" //21
                + "        for(int i = 0; i < 10; i++) {    \n" //22
                + "             a[i] = b[0];                \n" //23
                + "             d[i] = c[2];                \n" //24
                + "             b[i] = d[2];                \n" //25
                + "             c[i] = a[i];                \n" //26
                + "        }                                \n" //27
                + "    }                                    \n" //28
                + "}");
		IASTFunctionDefinition func = ASTUtil.findFirst(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		InferCopyin infer = new InferCopyin(stmts);
		Map<IASTStatement, Set<IBinding>> copyins = infer.get(); 
		for(IASTStatement stmt : copyins.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(copyins, stmt, "a"));
				assertTrue(copyins.get(stmt).size() == 1);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 9:
						assertTrue(containsBinding(copyins, stmt, "c"));
						assertTrue(copyins.get(stmt).size() == 1);
						break;
					case 12:
						assertTrue(containsBinding(copyins, stmt, "b"));
						assertTrue(copyins.get(stmt).size() == 1);
						break;
					case 22:
						assertTrue(containsBinding(copyins, stmt, "b"));
						assertTrue(containsBinding(copyins, stmt, "c"));
						assertTrue(containsBinding(copyins, stmt, "d"));
						assertTrue(containsBinding(copyins, stmt, "a"));
						assertTrue(copyins.get(stmt).size() == 4);
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	private boolean containsBinding(Map<IASTStatement, Set<IBinding>> copyins, IASTStatement statement, String binding) {
		Set<IBinding> bindings = copyins.get(statement);
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
