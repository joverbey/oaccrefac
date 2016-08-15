/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ptp.pldt.internal.tests.analyses;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.PointsToAnalysis;
import org.junit.Test;

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
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    int* a;",
                "    int* b;",
                "}"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
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
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    int* restrict a;",
                "    int* restrict b;",
                "    int* c;",
                "}"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        IVariable c = TestUtilities.findVariable(function, "c");
        assertNotNull(c);
        assertFalse(analysis.variablesMayPointToSame(a, b));
        assertFalse(analysis.variablesMayPointToSame(a, c));
    }
    
    /**
     * checkFunctionParameters checks that function pointers are also found.
     * 
     * @throws CoreException
     */
    @Test
    public void checkFunctionParameters() throws CoreException {
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo(int* restrict a, int* restrict b, int* c, int* d) { }"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        IVariable c = TestUtilities.findVariable(function, "c");
        assertNotNull(c);
        IVariable d = TestUtilities.findVariable(function, "d");
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
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    const int* a;",
                "    int const * b;",
                "    int* const c;",
                "}"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        IVariable c = TestUtilities.findVariable(function, "c");
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
        IASTFunctionDefinition function = TestUtilities.makeFunction(
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
        IASTFunctionDefinition dummyFunction = TestUtilities.makeFunction(
                "void foo(int* a, int* b) { }"
        );
        assertNotNull(dummyFunction);
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() { }"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = TestUtilities.findVariable(dummyFunction, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(dummyFunction, "b");
        assertNotNull(b);
        analysis.variablesMayPointToSame(a, b);
    }
    
    /**
     * checkOneRestrict checks that an IVariable that is restrict and another
     * that isn't can't point to the same variable.
     * 
     * @throws CoreException
     */
    @Test
    public void checkOneRestrict() throws CoreException {
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo(int* restrict a, int* b) { }"
        );
        assertNotNull(function);
        PointsToAnalysis analysis = new PointsToAnalysis(function, null);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        assertFalse(analysis.variablesMayPointToSame(a, b));
    }

}
