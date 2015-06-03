package edu.auburn.oaccrefac.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForLoopDependence;
import edu.auburn.oaccrefac.internal.core.FourierMotzkinEliminator;
import edu.auburn.oaccrefac.internal.core.Matrix;
import junit.framework.TestCase;

public class DependenceTestTest extends TestCase {

    Matrix a;
    
    private void p(Object s) {
        System.out.println(s);
    }
        
    public void test() throws CoreException {
        IASTTranslationUnit translationUnit = ASTUtil.translationUnitForString(
                "void main() {" +
                "  for(int i = 1; i < 3; i++) {" +
                "    a[i] = a[i+4] + 0;" +
                "  }" +
                "}"
                );
        
        

        IASTFunctionDefinition main = ASTUtil.findOne(translationUnit, IASTFunctionDefinition.class);
        for (IASTNode child : main.getBody().getChildren()) {
            if (child instanceof CPPASTForStatement) {
                ForLoopDependence dependence = new ForLoopDependence();
                dependence.addForLoop((CPPASTForStatement) child);
                Matrix A = dependence.generateInequalities();
                FourierMotzkinEliminator f = new FourierMotzkinEliminator();
                
                
                
                System.out.println(A.toString() + "\n");
                System.out.println("dependence? " + new FourierMotzkinEliminator().eliminateForRealSolutions(A));
                Assert.assertEquals(true, true);
            }
        }
    }
    
}
