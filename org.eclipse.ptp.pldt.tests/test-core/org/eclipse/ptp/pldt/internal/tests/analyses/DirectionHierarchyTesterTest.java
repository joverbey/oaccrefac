/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.internal.tests.analyses;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.ptp.pldt.openacc.internal.core.dependence.Direction;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.DirectionHierarchyTester;
import org.junit.Assert;
import org.junit.Test;

/**
 * the expectedSize parameter on setupAndAssertSubsetAndSize() may change if code changes
 */
public class DirectionHierarchyTesterTest {

    /**
     * for(int i = 0; i < 100; i++) {
     *     for(int j = 0; j < 100; j++) {
     *         a[0][1][0] = ...
     *         a[1][0][1] = ...
     *     }
     * } 
     * 
     */
    
    int[] lb = {1, 1};
    int[] ub = {100, 100};
    int[][] wc = {{0, 1, 0}, {0, 0, 1}};
    
    @Test
    public void test100_001LTLT() {
        int[][] rc = {{-1, 1, 0}, {-1, 0, 1}};
        Direction[] ex = {Direction.LT, Direction.LT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 1);
    }
    
    @Test
    public void test100_002LTEQ() {
        int[][] rc = {{-1, 1, 0}, {0, 0, 1}};
        Direction[] ex = {Direction.LT, Direction.EQ};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 1);
    }
    
    @Test
    public void test100_003LTGT() {
        int[][] rc = {{-1, 1, 0}, {1, 0, 1}};
        Direction[] ex = {Direction.LT, Direction.GT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 1);
    }
    
    @Test
    public void test100_004EQLT() {
        int[][] rc = {{0, 1, 0}, {-1, 0, 1}};
        Direction[] ex = {Direction.EQ, Direction.LT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 1);
    }
    
    @Test
    public void test100_005EQEQ() {
        int[][] rc = {{0, 1, 0}, {0, 0, 1}};
        Direction[] ex = {Direction.EQ, Direction.EQ};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 1);
    }
    
    @Test
    public void test100_006EQGT() {
        int[][] rc = {{0, 1, 0}, {1, 0, 1}};
        //should remove all dv's that have the first non-= as >, so no results
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, null, 0);
    }
    
    @Test
    public void test100_007GTLT() {
        int[][] rc = {{1, 1, 0}, {-1, 0, 1}};
        //should remove all dv's that have the first non-= as >, so no results
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, null, 0);
    }

    @Test
    public void test100_008GTEQ() {
        int[][] rc = {{1, 1, 0}, {0, 0, 1}};
        //should remove all dv's that have the first non-= as >, so no results
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, null, 0);
    }
    
    @Test
    public void test100_009GTGT() {
        int[][] rc = {{1, 1, 0}, {1, 0, 1}};
        //should remove all dv's that have the first non-= as >, so no results
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, null, 0);
    }
    
    private void setupAndAssertSubsetAndSize(int[] lb, int[] ub, int[][] wc, int[][] rc, Direction[] expected, int expectedSize) {
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Set<Direction[]> actual = tester.getPossibleDependenceDirections();
        if(expected != null) {
            Assert.assertTrue(getStringsFromDVs(actual).contains(stringifyDV(expected)));
        }
        else {
            Assert.assertTrue(actual.isEmpty());
        }
        Assert.assertTrue(actual.size() == expectedSize);
    }
    
    private String stringifyDV(Direction[] v) {
        StringBuilder sb = new StringBuilder();
        for(Direction el : v) {
            sb.append(el);
            sb.append(" ");
        }
        return sb.toString();
    }
    
    private Set<String> getStringsFromDVs(Set<Direction[]> dvs) {
        Set<String> strs = new HashSet<String>();
        for(Direction[] dv : dvs) {
            strs.add(stringifyDV(dv));
        }
        return strs;
    }
    
}
