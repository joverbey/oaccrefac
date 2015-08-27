/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.core.tests;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.auburn.oaccrefac.core.dependence.Direction;
import edu.auburn.oaccrefac.internal.core.dependence.DirectionHierarchyTester;

public class DirectionHierarchyTesterTest {

    int[] lb = {1, 1};
    int[] ub = {100, 100};
    int[][] wc = {{0, 1, 0}, {0, 0, 1}};
    
    @Test
    public void test100_001LTLT() {
        int[][] rc = {{-1, 1, 0}, {-1, 0, 1}};
        Direction[] ex = {Direction.LT, Direction.LT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 3);
    }
    
    @Test
    public void test100_002LTEQ() {
        int[][] rc = {{-1, 1, 0}, {0, 0, 1}};
        Direction[] ex = {Direction.LT, Direction.EQ};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 4);
    }
    
    @Test
    public void test100_003LTGT() {
        int[][] rc = {{-1, 1, 0}, {1, 0, 1}};
        Direction[] ex = {Direction.LT, Direction.GT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 5);
    }
    
    @Test
    public void test100_004EQLT() {
        int[][] rc = {{0, 1, 0}, {-1, 0, 1}};
        Direction[] ex = {Direction.EQ, Direction.LT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 4);
    }
    
    @Test
    public void test100_005EQEQ() {
        int[][] rc = {{0, 1, 0}, {0, 0, 1}};
        Direction[] ex = {Direction.EQ, Direction.EQ};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 3);
    }
    
    @Test
    public void test100_006EQGT() {
        int[][] rc = {{0, 1, 0}, {1, 0, 1}};
        Direction[] ex = {Direction.EQ, Direction.GT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 4);
    }
    
    @Test
    public void test100_007GTLT() {
        int[][] rc = {{1, 1, 0}, {-1, 0, 1}};
        Direction[] ex = {Direction.GT, Direction.LT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 5);
    }

    @Test
    public void test100_008GTEQ() {
        int[][] rc = {{1, 1, 0}, {0, 0, 1}};
        Direction[] ex = {Direction.GT, Direction.EQ};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 4);
    }
    
    @Test
    public void test100_009GTGT() {
        int[][] rc = {{1, 1, 0}, {1, 0, 1}};
        Direction[] ex = {Direction.GT, Direction.GT};
        setupAndAssertSubsetAndSize(lb, ub, wc, rc, ex, 3);
    }
    
    private void setupAndAssertSubsetAndSize(int[] lb, int[] ub, int[][] wc, int[][] rc, Direction[] expectedVector, int expectedSize) {
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Set<Direction[]> actual = tester.getPossibleDependenceDirections();
        //printResults(ex, actual);
        Assert.assertTrue(containsAll(getStringsFromDVs(actual), getStringsFromDVs(ex)));
        Assert.assertTrue(actual.size() == expectedSize);
    }
    
    private void printResults(Set<Direction[]> expected, Set<Direction[]> actual) {
        System.out.println("--------------------");
        System.out.println("Expected: ");
        ps(expected);
        System.out.println("Got: ");
        ps(actual);
        System.out.println("--------------------");
    }
    
    private void ps(Set<Direction[]> s) {
        for(Direction[] dv : s) {
            System.out.println(stringifyDV(dv));
        }
        System.out.println();
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
    
    private boolean containsAll(Set<String> s1, Set<String> s2) {
        for(String el2 : s2) {
            boolean el2InS1 = false;
            for(String el1 : s1) {
                if(el1.equals(el2)) {
                    el2InS1 = true;
                    break;
                }
            }
            if(!el2InS1) {
                return false;
            }
        }
        return true;
    }
    
}
