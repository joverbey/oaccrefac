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
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import edu.auburn.oaccrefac.core.dependence.AddressTakenAnalysis;

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
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    int a = 0;",
                "    int* b = &a;",
                "}"
        );
        assertNotNull(function);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, null);
        assertTrue(analysis.isAddressTaken(a));
        assertFalse(analysis.isAddressTaken(b));
    }

    /**
     * checkNested checks that address of analysis can find a nested binding.
     * 
     * @throws CoreException
     */
    @Test
    public void checkNested() throws CoreException {
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    int a = 0;",
                "    int* b = &(((a)));",
                "}"
        );
        assertNotNull(function);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, null);
        assertTrue(analysis.isAddressTaken(a));
        assertFalse(analysis.isAddressTaken(b));
    }
    
    /**
     * checkUnaryExpression checks that address of can find a binding that
     * had a unary expression applied to it.
     * 
     * @throws CoreException
     */
    @Test
    public void checkUnary() throws CoreException {
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    int* a;",
                "    int* b = &*a++;",
                "}"
        );
        assertNotNull(function);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, null);
        assertTrue(analysis.isAddressTaken(a));
        assertFalse(analysis.isAddressTaken(b));
    }
    
    /**
     * checkComplicated run the analysis on a complicated expression that
     * contains casts.
     * 
     * @throws CoreException
     */
    @Test
    public void checkComplicated() throws CoreException {
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    int* a;",
                "    int* b;",
                "    int c = 0;",
                "    int d = 0;",
                "    int* e = &*(c + d + (long) b + a);",
                "}"
        );
        assertNotNull(function);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        IVariable c = TestUtilities.findVariable(function, "c");
        assertNotNull(c);
        IVariable d = TestUtilities.findVariable(function, "d");
        assertNotNull(d);
        IVariable e = TestUtilities.findVariable(function, "e");
        assertNotNull(e);
        AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, null);
        assertTrue(analysis.isAddressTaken(a));
        assertFalse(analysis.isAddressTaken(b));
        assertFalse(analysis.isAddressTaken(c));
        assertFalse(analysis.isAddressTaken(d));
        assertFalse(analysis.isAddressTaken(e));
    }
    
    /**
     * checkNestedAddresses checks that bindings in nested address of statements
     * are found.
     * 
     * @throws CoreException
     */
    @Test
    public void checkNestedAddresses() throws CoreException {
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    int* a;",
                "    int* b;",
                "    int c = 0;",
                "    int* d = &*(a + (long) &*(b + c));",
                "}"
        );
        assertNotNull(function);
        IVariable a = TestUtilities.findVariable(function, "a");
        assertNotNull(a);
        IVariable b = TestUtilities.findVariable(function, "b");
        assertNotNull(b);
        IVariable c = TestUtilities.findVariable(function, "c");
        assertNotNull(c);
        IVariable d = TestUtilities.findVariable(function, "d");
        assertNotNull(d);
        AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, null);
        assertTrue(analysis.isAddressTaken(a));
        assertTrue(analysis.isAddressTaken(b));
        assertFalse(analysis.isAddressTaken(c));
        assertFalse(analysis.isAddressTaken(d));
    }
    
    /**
     * checkNullFunctionInConstructor checks that an exception is thrown when
     * a null function is passed to the AddressTakenAnalysis constructor.
     */
    @Test(expected=IllegalArgumentException.class)
    public void checkNullFunctionInConstructor() {
        AddressTakenAnalysis.forFunction(null, null);
    }
    
    /**
     * checkNullVariable checks that an exception is thrown when a null
     * variable is passed to addressTaken.
     * 
     * @throws CoreException
     */
    @Test(expected=IllegalArgumentException.class)
    public void checkNullVariable() throws CoreException {
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() { }"
        );
        assertNotNull(function);
        AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, null);
        analysis.isAddressTaken(null);
    }
    
    /**
     * checkNotLocalVariable checks that an exception is thrown when a given
     * variable isn't local.
     * 
     * @throws CoreException
     */
    @Test(expected=IllegalArgumentException.class)
    public void checkNotLocalVariable() throws CoreException {
        IASTFunctionDefinition dummyFunction = TestUtilities.makeFunction(
                "void foo(int a) { }"
        );
        assertNotNull(dummyFunction);
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() { }"
        );
        assertNotNull(function);
        AddressTakenAnalysis analysis = AddressTakenAnalysis.forFunction(function, null);
        IVariable a = TestUtilities.findVariable(dummyFunction, "a");
        assertNotNull(a);
        analysis.isAddressTaken(a);
    }
    
    /**
     * checkBadExpression checks the address of a non LValue expression can't
     * be taken.
     * 
     * @throws CoreException
     */
    @Test(expected=IllegalArgumentException.class)
    public void checkBadExpression() throws CoreException {
        IASTFunctionDefinition function = TestUtilities.makeFunction(
                "void foo() {",
                "    int* a;",
                "    int* b;",
                "    int* c = &(a + b);",
                "}"
        );
        assertNotNull(function);
        AddressTakenAnalysis.forFunction(function, null);
    }

}
