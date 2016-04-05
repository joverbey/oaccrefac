package org.eclipse.ptp.pldt.internal.tests.analyses;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class TestUtilities {
    
    /**
     * makeFunction converts a lines representing a function into an
     * IASTFunctionDefinition.
     * 
     * @param lines Function.
     * @return IASTFunctionDefinition made out of the lines.
     * @throws CoreException
     */
    public static IASTFunctionDefinition makeFunction(String... lines) throws CoreException {
        String function = "";
        for (String line : lines) {
            function += line + "\n";
        }
        function = function.substring(0, function.length()-1);
        IASTTranslationUnit translationUnit = ASTUtil.translationUnitForString(function);
        return ASTUtil.findOne(translationUnit, IASTFunctionDefinition.class);
    }
    
    /**
     * findVariable finds a variable in the given IASTNode.
     * 
     * @param current IASTNode to look in.
     * @param variableName IVariable being searched for.
     * @return IVariable with given name if found.
     */
    public static IVariable findVariable(IASTNode current, final String variableName) {
        if (current instanceof IASTName) {
            IBinding binding = ((IASTName) current).resolveBinding();
            if (binding instanceof IVariable && binding.getName().equals(variableName)) {
                return (IVariable) binding;
            }
        }
        for (IASTNode child : current.getChildren()) {
           IVariable found = findVariable(child, variableName);
           if (found != null) {
               return found;
           }
        }
        return null;
    }
    
}
