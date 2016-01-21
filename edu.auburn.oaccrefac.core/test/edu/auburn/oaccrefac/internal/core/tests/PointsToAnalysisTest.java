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


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.auburn.oaccrefac.core.dependence.PointsToAnalysis;

/**
 * Unit tests for {@link PointsToAnalysis}.
 * 
 * Test various cases that should or should not work for the PointsToAnalysis.
 */
public class PointsToAnalysisTest {
    
    /**
     * Checks that the points-to-analysis works for a simple address-of.
     * 
     * int a = 1;
     * int* b = &a;
     *
     * a may be aliased.
     */
    @Test
    public void checkSimpleAddressOf() {
   
    }
    
    /**
     * Checks that the points-to-analysis works for a nested address-of.
     * 
     * int a = 1;
     * int* b = (((&(((a))))));
     * 
     * a may be aliased.
     */
    @Test
    public void checkNestedAddressOf() {
        assertTrue(false);
    }
    
    /**
     * Checks that the points-to-analysis works on operators that return l-values.
     * 
     * int a = 1
     * int* b = &(a++);
     * 
     * a may be aliased.
     */
    @Test
    public void checkOperatorAddressOf() {
        assertTrue(false);
    }
    
    /**
     * Checks that the points-to-analysis works on adress-of dereference chains.
     * 
     * int* a = (int*) malloc(sizeof(int));
     * int* b = &*a;
     * 
     * a may be aliased.
     */
    @Test
    public void checkAddressOfDereference() {
        assertTrue(false);
    }
    
    /**
     * Checks that the points-to-analysis works on structs.
     * 
     * typedef struct {
     *   int x;
     * } A;
     * A a;
     * A* b = &a;
     * 
     * a may be aliased.
     */
    @Test
    public void checkStructAddressOf() {
        assertTrue(false);
    }
    
    /**
     * Checks that the points-to-analysis works on struct members.
     * 
     * typdef struct {
     *   int x;
     * } A;
     * A a;
     * int* b = &a.x;
     * 
     * a.x may be aliased.
     */
    @Test
    public void checkStructMemberAddressOf() {
        assertTrue(false);
    }
    
    /**
     * Checks that the points-to-analysis says global variables aren't aliased.
     * 
     * int a = 1;
     * int foo() {
     *   int b = &a;
     * }
     * 
     * Exception thrown.
     */
    @Test
    public void checkDontIncludeGlobals() {
        assertTrue(false);
    }

}
