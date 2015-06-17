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
    
    
    private void ps(Set<Direction[]> s) {
        for(Direction[] dv : s) {
            for(Direction d : dv) {
                System.out.print("" + d + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
    @Test
    public void test100_001LTLT() {
        int[][] rc = {{-1, 1, 0}, {-1, 0, 1}};
        Direction[] expectedVector = {Direction.LT, Direction.LT};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        ps(ex);
        ps(tester.getPossibleDependenceDirections());
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }
    
    @Test
    public void test100_002LTEQ() {
        int[][] rc = {{-1, 1, 0}, {0, 0, 1}};
        Direction[] expectedVector = {Direction.LT, Direction.EQ};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }
    
    @Test
    public void test100_003LTGT() {
        int[][] rc = {{-1, 1, 0}, {1, 0, 1}};
        Direction[] expectedVector = {Direction.LT, Direction.GT};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }
    
    @Test
    public void test100_004EQLT() {
        int[][] rc = {{0, 1, 0}, {-1, 0, 1}};
        Direction[] expectedVector = {Direction.EQ, Direction.LT};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }
    
    @Test
    public void test100_005EQEQ() {
        int[][] rc = {{0, 1, 0}, {0, 0, 1}};
        Direction[] expectedVector = {Direction.EQ, Direction.EQ};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }
    
    @Test
    public void test100_006EQGT() {
        int[][] rc = {{0, 1, 0}, {1, 0, 1}};
        Direction[] expectedVector = {Direction.EQ, Direction.GT};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }
    
    @Test
    public void test100_007GTLT() {
        int[][] rc = {{1, 1, 0}, {-1, 0, 1}};
        Direction[] expectedVector = {Direction.GT, Direction.LT};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }

    @Test
    public void test100_008GTEQ() {
        int[][] rc = {{1, 1, 0}, {0, 0, 1}};
        Direction[] expectedVector = {Direction.GT, Direction.EQ};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }
    
    @Test
    public void test100_009GTGT() {
        int[][] rc = {{1, 1, 0}, {1, 0, 1}};
        Direction[] expectedVector = {Direction.GT, Direction.GT};
        Set<Direction[]> ex = new HashSet<Direction[]>();
        ex.add(expectedVector);
        DirectionHierarchyTester tester = new DirectionHierarchyTester(lb, ub, wc, rc, 0);
        Assert.assertTrue(ex.equals(tester.getPossibleDependenceDirections()));
    }
    
}
