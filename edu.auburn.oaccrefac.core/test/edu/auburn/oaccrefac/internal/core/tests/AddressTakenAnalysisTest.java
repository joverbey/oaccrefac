/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

package edu.auburn.oaccrefac.internal.core.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import edu.auburn.oaccrefac.core.dependence.AddressTakenAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Unit tests for {@link AddressTakenAnalysis}.
 * 
 * Test various cases that should or should not work for the AddressTakenAnalysis.
 */
public class AddressTakenAnalysisTest {

    /**
     * checkSimpleAddress checks that the analysis works in the simplest case
     * of just taking the address of a single variable.
     * 
     * @throws CoreException
     */
    @Test
    public void checkSimpleAddress() throws CoreException {
        IASTFunctionDefinition function = makeFunction(
                "void foo() {",
                "    int a = 0;",
                "    int* b = &a;",
                "}"
        );
        assertNotNull(function);
        IVariable a = findVariable(function, "a");
        assertNotNull(a);
        IVariable b = findVariable(function, "b");
        assertNotNull(b);
        AddressTakenAnalysis analysis = new AddressTakenAnalysis(function, null);
        assertFalse(analysis.addressTaken(a));
        assertTrue(analysis.addressTaken(b));
    }
    
    // check nulls in constructor
    
    // check nulls in addressTaken
    
    // check nested expression
    
    // check complicated expression
    
    // check not local variable
    
    // check illegal expressionmm 
    
    /**
     * makeFunction converts a lines representing a function into an
     * IASTFunctionDefinition.
     * 
     * @param lines Function.
     * @return IASTFunctionDefinition made out of the lines.
     * @throws CoreException
     */
    private IASTFunctionDefinition makeFunction(String... lines) throws CoreException {
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
    private IVariable findVariable(IASTNode current, final String variableName) {
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
