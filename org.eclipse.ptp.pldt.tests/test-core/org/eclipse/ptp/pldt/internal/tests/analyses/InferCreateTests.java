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
import org.eclipse.ptp.pldt.openacc.core.dataflow.InferCreate;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.patternmatching.ArbitraryStatement;

import junit.framework.TestCase;

public class InferCreateTests extends TestCase {

	public void testNothing() throws CoreException {
		IASTTranslationUnit tu = ASTUtil.translationUnitForString(""
                + "void main() {                    \n"
                + "    #pragma acc parallel loop    \n"
                + "    for(int i = 0; i < 10; i++) {\n"
                + "        ;//do nothing            \n"
                + "    }                            \n"
                + "}");
		IASTFunctionDefinition func = ASTUtil.findOne(tu, IASTFunctionDefinition.class);
		IASTStatement[] stmts = ((IASTCompoundStatement) func.getBody()).getStatements();
		Map<IASTStatement, Set<IBinding>> copyins = new InferCopyin(stmts).get(); 
		for(IASTStatement stmt : copyins.keySet()) {
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
		IASTFunctionDefinition func = ASTUtil.findOne(tu, IASTFunctionDefinition.class);
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
                + "    int a;                           \n" //2
                + "    int b;                           \n" //3
                + "	   #pragma acc data copyin(a, b)    \n" //4
                + "	   {                                \n" //5
                + "        #pragma acc parallel loop    \n" //6
                + "        for(int i = 0; i < 10; i++) {\n" //7
                + "             int tmp = a;            \n" //8
                + "             b = a;                  \n" //9
                + "             a = b;                  \n" //10
                + "             a = tmp;                \n" //11
                + "        }                            \n" //12
                + "    }                                \n" //13
                + "}");
		IASTFunctionDefinition func = ASTUtil.findOne(tu, IASTFunctionDefinition.class);
		IASTCompoundStatement outer = getFirstChildCompound(func.getBody());  
		IASTStatement[] stmts = outer.getStatements();
		Map<IASTStatement, Set<IBinding>> create = new InferCreate(stmts).get(); 
		for(IASTStatement stmt : create.keySet()) {
			if(stmt instanceof ArbitraryStatement) {
				assertTrue(containsBinding(create, stmt, "a"));
				assertTrue(containsBinding(create, stmt, "b"));
				assertTrue(create.get(stmt).size() == 2);
			}
			else {
				switch(stmt.getFileLocation().getStartingLineNumber()) {
					case 7:
						assertTrue(create.get(stmt).isEmpty());
						break;
					default:
						//we should be covering every possible case
						assertTrue(false);
						break;
				}
			}
		}
	}
	
	
	private boolean containsBinding(Map<IASTStatement, Set<IBinding>> creates, IASTStatement statement, String binding) {
		Set<IBinding> bindings = creates.get(statement);
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
