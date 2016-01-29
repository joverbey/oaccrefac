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

import edu.auburn.oaccrefac.core.dependence.PointsToAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Unit tests for {@link PointsToAnalysis}.
 * 
 * Test various cases that should or should not work for the PointsToAnalysis.
 */
public class PointsToAnalysisTest {
    
    /**
     * checkTwoPointers checks that the analysis works in the simple case of
     * two just two pointers.
     * 
     * @throws CoreException
     */
    @Test
    public void checkTwoPointers() throws CoreException {
        IASTFunctionDefinition function = makeFunction(
                "void foo() {",
                "    int* a;",
                "    int* b;",
                "}"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = findVariable(function, "a");
        assertNotNull(a);
        IVariable b = findVariable(function, "b");
        assertNotNull(b);
        assertTrue(analysis.variablesMayPointToSame(a, b));
    }
    
    /**
     * checkRestrictPointers checks that restrict pointers aren't said to
     * point to the same variable as any other pointer.
     * 
     * @throws CoreException
     */
    @Test
    public void checkRestrictPointers() throws CoreException {
        IASTFunctionDefinition function = makeFunction(
                "void foo() {",
                "    int* restrict a;",
                "    int* restrict b;",
                "    int* c;",
                "}"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = findVariable(function, "a");
        assertNotNull(a);
        IVariable b = findVariable(function, "b");
        assertNotNull(b);
        IVariable c = findVariable(function, "c");
        assertNotNull(c);
        assertFalse(analysis.variablesMayPointToSame(a, b));
        assertFalse(analysis.variablesMayPointToSame(a, c));
    }
    
    /**
     * checkFunctionParameters checks that function pointers are also found.
     * @throws CoreException
     */
    @Test
    public void checkFunctionParameters() throws CoreException {
        IASTFunctionDefinition function = makeFunction(
                "void foo(int* restrict a, int* restrict b, int* c, int* d) {",
                "}"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = findVariable(function, "a");
        assertNotNull(a);
        IVariable b = findVariable(function, "b");
        assertNotNull(b);
        IVariable c = findVariable(function, "c");
        assertNotNull(c);
        IVariable d = findVariable(function, "d");
        assertNotNull(d);
        assertFalse(analysis.variablesMayPointToSame(a, b));
        assertFalse(analysis.variablesMayPointToSame(a, c));
        assertTrue(analysis.variablesMayPointToSame(c, d));
    }
    
    /**
     * checksQualifiedParameters checks that the analysis still finds qualified
     * pointers.
     * 
     * @throws CoreException
     */
    @Test
    public void checkQualifiedParameters() throws CoreException {
        IASTFunctionDefinition function = makeFunction(
                "void foo() {",
                "    const int* a;",
                "    int const * b;",
                "    int* const c;",
                "}"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = findVariable(function, "a");
        assertNotNull(a);
        IVariable b = findVariable(function, "b");
        assertNotNull(b);
        IVariable c = findVariable(function, "c");
        assertNotNull(c);
        assertTrue(analysis.variablesMayPointToSame(a, b));
        assertTrue(analysis.variablesMayPointToSame(a, c));
    }
    
    /**
     * checkNullFunction checks that the constructor throws an exception if
     * function is null.
     */
    @Test(expected=IllegalArgumentException.class)
    public void checkNullFunction() {
        new PointsToAnalysis(null, null);
    }
    
    /**
     * checkNulls checks that null IVariables throw exceptions.
     * 
     * @throws CoreException
     */
    @Test(expected=IllegalArgumentException.class)
    public void checkNulls() throws CoreException {
        IASTFunctionDefinition function = makeFunction(
                "void foo() { }"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        analysis.variablesMayPointToSame(null, null);
    }
    
    /**
     * checkNotLocals checks that IVariables that aren't local variables throw
     * exceptions.
     * 
     * @throws CoreException
     */
    @Test(expected=IllegalArgumentException.class)
    public void checkNotLocals() throws CoreException {
        IASTFunctionDefinition dummyFunction = makeFunction(
                "void foo(int* a, int* b) { }"
        );
        assertNotNull(dummyFunction);
        IASTFunctionDefinition function = makeFunction(
                "void foo() { }"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = findVariable(dummyFunction, "a");
        assertNotNull(a);
        IVariable b = findVariable(dummyFunction, "b");
        assertNotNull(b);
        analysis.variablesMayPointToSame(a, b);
    }
    
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
