package edu.auburn.oaccrefac.internal.core;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;
import org.eclipse.core.runtime.CoreException;

public class LoopDependenceMain {
    public static void main(String[] args) throws CoreException {
        IASTTranslationUnit translationUnit = ASTUtil.translationUnitForFile("src-dependence-examples/example.cpp");

        IASTFunctionDefinition main = ASTUtil.findOne(translationUnit, IASTFunctionDefinition.class);
        for (IASTNode child : main.getBody().getChildren()) {
            if (child instanceof CPPASTForStatement) {
                ForLoopDependence dependence = new ForLoopDependence();
                dependence.addForLoop((CPPASTForStatement) child);
                Matrix A = dependence.generateInequalities();

                System.out.println(A.toString());
                System.out.println("dependence? " + new FourierMotzkinEliminator().eliminateForRealSolutions(A));
            }
        }
    }
}
