package edu.auburn.oaccrefac.core.newtmp;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.fromphotran.DependenceTestFailure;

@SuppressWarnings("restriction")
public class DependenceRunner {

    public static void main(String[] args) throws CoreException, DependenceTestFailure {
        IASTTranslationUnit translationUnit = ASTUtil.translationUnitForFile("src-dependence-examples/test.c");

        IASTFunctionDefinition main = ASTUtil.findOne(translationUnit, IASTFunctionDefinition.class);
        for (IASTNode child : main.getBody().getChildren()) {
            if (child instanceof CPPASTForStatement) {
                ForLoopDependenceSystem dependence = new ForLoopDependenceSystem((CPPASTForStatement) child);
                System.out.println(dependence);
            }
        }

    }
    
}
