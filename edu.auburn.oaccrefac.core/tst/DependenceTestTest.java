import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTForStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForLoopDependence;
import edu.auburn.oaccrefac.internal.core.FourierMotzkinEliminator;
import edu.auburn.oaccrefac.internal.core.Matrix;
import org.junit.Assert;

public class DependenceTestTest {

	private Matrix getInequalitiesFromCodeString(String code) {
		IASTTranslationUnit translationUnit = null;
		try {
			translationUnit = ASTUtil.translationUnitForString(code);
		} catch (CoreException e) {
			System.err.println("getInequalitiesFromCodeString - caught exception");
			return null;
		}
        IASTFunctionDefinition main = ASTUtil.findOne(translationUnit, IASTFunctionDefinition.class);
        ForLoopDependence dep = new ForLoopDependence();
        for (IASTNode child : main.getBody().getChildren()) {
            if (child instanceof CPPASTForStatement) {
            	dep.addForLoop((CPPASTForStatement) child);
            	break;
            }
        }
        return dep.generateInequalities();
	}
	
    @Test
    public void testMatrix1()  {
    	String code =
    			"void main() {\n                   " + //
    			"  int a[5];\n                     " + //
    			"  int b[5];\n                     " + //
    			"  for (int i = 0; i < 5; i++) {\n " + //
    			"    a[i] = b[i] + 0;\n            " + //
    			"  }\n                             " + //
    			"}                                 ";
       Matrix a = getInequalitiesFromCodeString(code);
       Matrix b = getInequalitiesFromCodeString(code);
       Assert.assertEquals(a, b);
    }
    
    @Test
    public void testMatrix2()  {
    	String code =
    			"void main() {\n                   " + //
    			"  int a[5];\n                     " + //
    			"  int b[5];\n                     " + //
    			"  for (int i = 0; i < 5; i++) {\n " + //
    			"    a[i] = b[i] + 0;\n            " + //
    			"  }\n                             " + //
    			"}                                 ";
       Matrix a = getInequalitiesFromCodeString(code);
       Matrix b = getInequalitiesFromCodeString(code);
       Assert.assertEquals(a, b);
    }
    
    @Test
    public void testMatrix3()  {
    	String code =
    			"void main() {\n                   " + //
    			"  int a[5];\n                     " + //
    			"  int b[5];\n                     " + //
    			"  for (int i = 0; i < 5; i++) {\n " + //
    			"    a[i] = b[i] + 0;\n            " + //
    			"  }\n                             " + //
    			"}                                 "; 
    	Matrix a = getInequalitiesFromCodeString(code);
        Matrix b = getInequalitiesFromCodeString(code);
        b.setSingleRow(2, new double[] {2.0, 2.0, 2.0});
        Assert.assertEquals(a.equals(b), false);
    }
    
}
 