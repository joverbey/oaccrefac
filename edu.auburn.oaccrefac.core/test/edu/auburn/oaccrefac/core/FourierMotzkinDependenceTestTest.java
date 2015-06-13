package edu.auburn.oaccrefac.core;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.auburn.oaccrefac.core.newtmp.Direction;
import edu.auburn.oaccrefac.core.newtmp.FourierMotzkinDependenceTest;
import edu.auburn.oaccrefac.internal.core.Matrix;

/**
 * Unit tests for FourierMotzkinDependenceTest class
 * 
 * the test() method is not tested here because it is simply a wrapper 
 *  for generateDependenceMatrix() and eliminateForIntegerSolutions() 
 *  from the FM eliminator class  
 * 
 * @author Alexander Calvert
 * 
 */
public class FourierMotzkinDependenceTestTest {

    /*
     * tests FourierMotzkinDependenceTest class
     * 
     * method header:
     * public boolean test(int nestingLevel, int[] lowerBounds, int[] upperBounds, 
     *      int[][] writeCoefficients, int[][] readCoefficients, Direction[] direction)
     */
    FourierMotzkinDependenceTest tester = new FourierMotzkinDependenceTest();

    //in a loop dependence, it must be the case that 
    // d.length == lb.length == ub.length == wd.length == rc.length == wc[x].length-1 == rc[x].length-1
    int[] lb1 = {1, 1};
    int[] lb2 = {1, 1};
    int[] lb3 = {1, 1};
    int[] lb4 = {10, 10};
    int[] lb5 = {1, 1};
    int[] lb6 = {1, 1};
    int[] lb7 = {1, 1};
    int[] lb8 = {1, 1};
    int[] lb9 = {1, 1};
    int[] lb10 = {1, 1};
    int[] lb11 = {1, 1};
    int[] lb12 = {1};
    
    int[] ub1 = {100, 100};
    int[] ub2 = {10000, 10000};
    int[] ub3 = {1, 1};
    int[] ub4 = {1, 1};
    int[] ub5 = {100, 100};
    int[] ub6 = {100, 100};
    int[] ub7 = {100, 100};
    int[] ub8 = {100, 100};
    int[] ub9 = {100, 100};
    int[] ub10 = {100, 100};
    int[] ub11 = {100, 100};
    int[] ub12 = {100};
    
    int[][] wc1 = {{2, 1, 1}, {2, 1, 1}};
    int[][] wc2 = {{2, 1, 1}, {2, 1, 1}};
    int[][] wc3 = {{2, 1, 1}, {2, 1, 1}};
    int[][] wc4 = {{2, 1, 1}, {2, 1, 1}};
    int[][] wc5 = {{2, 0, 0}, {2, 0, 0}};
    int[][] wc6 = {{2, 1, 1}, {2, 1, 1}};
    int[][] wc7 = {{2, -1, -1}, {2, -1, -1}};
    int[][] wc8 = {{2, 1, 1}, {2, 1, 1}};
    int[][] wc9 = {{2, 5, 5}, {2, 5, 5}};
    int[][] wc10 = {{2, 5, 5}, {2, 5, 5}};
    int[][] wc11 = {{2, 5, 5}, {2, 5, 5}};
    int[][] wc12 = {{2, 5}};
    
    
    int[][] rc1 = {{4, 1, 1}, {4, 1, 1}};
    int[][] rc2 = {{4, 1, 1}, {4, 1, 1}};
    int[][] rc3 = {{4, 1, 1}, {4, 1, 1}};
    int[][] rc4 = {{4, 1, 1}, {4, 1, 1}};
    int[][] rc5 = {{4, 1, 1}, {4, 1, 1}};
    int[][] rc6 = {{4, 0, 0}, {4, 0, 0}};
    int[][] rc7 = {{4, 1, 1}, {4, 1, 1}};
    int[][] rc8 = {{4, -1, -1}, {4, -1, -1}};
    int[][] rc9 = {{4, 5, 5}, {4, 5, 5}};
    int[][] rc10 = {{4, 5, 5}, {4, 5, 5}};
    int[][] rc11 = {{4, 5, 5}, {4, 5, 5}};
    int[][] rc12 = {{4, 5}};
    
    Direction[] d1 = {Direction.ANY, Direction.ANY};
    Direction[] d2 = {Direction.ANY, Direction.ANY};
    Direction[] d3 = {Direction.ANY, Direction.ANY};
    Direction[] d4 = {Direction.ANY, Direction.ANY};
    Direction[] d5 = {Direction.ANY, Direction.ANY};
    Direction[] d6 = {Direction.ANY, Direction.ANY};
    Direction[] d7 = {Direction.ANY, Direction.ANY};
    Direction[] d8 = {Direction.ANY, Direction.ANY};
    Direction[] d9 = {Direction.LT, Direction.LT};
    Direction[] d10 = {Direction.GT, Direction.GT};
    Direction[] d11 = {Direction.EQ, Direction.EQ};
    Direction[] d12 = {Direction.ANY};
    
    @Test
    public void test100_001SimpleCorrectMatrix() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {1, 1, -1, -1, 2});
        rowsExpected.add(new double[] {-1, -1, 1, 1, -2});
        rowsExpected.add(new double[] {1, 1, -1, -1, 2});
        rowsExpected.add(new double[] {-1, -1, 1, 1, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 100});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 100});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 100});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 100});
        Matrix expected = new Matrix(rowsExpected);     
        Matrix result = tester.generateDependenceMatrix(lb1, ub1, wc1, rc1, d1);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_002LargeUpperBounds() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {1, 1, -1, -1, 2});
        rowsExpected.add(new double[] {-1, -1, 1, 1, -2});
        rowsExpected.add(new double[] {1, 1, -1, -1, 2});
        rowsExpected.add(new double[] {-1, -1, 1, 1, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 10000});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 10000});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 10000});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 10000});
        Matrix expected = new Matrix(rowsExpected);     
        Matrix result = tester.generateDependenceMatrix(lb2, ub2, wc2, rc2, d2);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_003UBEqualsLB() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {1, 1, -1, -1, 2});
        rowsExpected.add(new double[] {-1, -1, 1, 1, -2});
        rowsExpected.add(new double[] {1, 1, -1, -1, 2});
        rowsExpected.add(new double[] {-1, -1, 1, 1, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 1});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 1});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 1});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 1});
        Matrix expected = new Matrix(rowsExpected);  
        Matrix result = tester.generateDependenceMatrix(lb3, ub3, wc3, rc3, d3);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_004UBLessThanLB() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {1, 1, -1, -1, 2});
        rowsExpected.add(new double[] {-1, -1, 1, 1, -2});
        rowsExpected.add(new double[] {1, 1, -1, -1, 2});
        rowsExpected.add(new double[] {-1, -1, 1, 1, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -10});
        rowsExpected.add(new double[] {1, 0, 0, 0, 1});
        rowsExpected.add(new double[] {0, -1, 0, 0, -10});
        rowsExpected.add(new double[] {0, 1, 0, 0, 1});
        rowsExpected.add(new double[] {0, 0, -1, 0, -10});
        rowsExpected.add(new double[] {0, 0, 1, 0, 1});
        rowsExpected.add(new double[] {0, 0, 0, -1, -10});
        rowsExpected.add(new double[] {0, 0, 0, 1, 1});
        Matrix expected = new Matrix(rowsExpected);       
        Matrix result = tester.generateDependenceMatrix(lb4, ub4, wc4, rc4, d4);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_005ZeroWriteCoefficients() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {0, 0, -1, -1, 2});
        rowsExpected.add(new double[] {0, 0, 1, 1, -2});
        rowsExpected.add(new double[] {0, 0, -1, -1, 2});
        rowsExpected.add(new double[] {0, 0, 1, 1, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 100});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 100});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 100});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 100});
        Matrix expected = new Matrix(rowsExpected);       
        Matrix result = tester.generateDependenceMatrix(lb5, ub5, wc5, rc5, d5);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_006ZeroReadCoefficients() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {1, 1, 0, 0, 2});
        rowsExpected.add(new double[] {-1, -1, 0, 0, -2});
        rowsExpected.add(new double[] {1, 1, 0, 0, 2});
        rowsExpected.add(new double[] {-1, -1, 0, 0, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 100});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 100});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 100});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 100});
        Matrix expected = new Matrix(rowsExpected);       
        Matrix result = tester.generateDependenceMatrix(lb6, ub6, wc6, rc6, d6);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_007NegativeWriteCoefficients() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {-1, -1, -1, -1, 2});
        rowsExpected.add(new double[] {1, 1, 1, 1, -2});
        rowsExpected.add(new double[] {-1, -1, -1, -1, 2});
        rowsExpected.add(new double[] {1, 1, 1, 1, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 100});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 100});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 100});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 100});
        Matrix expected = new Matrix(rowsExpected);       
        Matrix result = tester.generateDependenceMatrix(lb7, ub7, wc7, rc7, d7);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_008NegativeReadCoefficients() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {1, 1, 1, 1, 2});
        rowsExpected.add(new double[] {-1, -1, -1, -1, -2});
        rowsExpected.add(new double[] {1, 1, 1, 1, 2});
        rowsExpected.add(new double[] {-1, -1, -1, -1, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 100});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 100});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 100});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 100});
        Matrix expected = new Matrix(rowsExpected);   
        Matrix result = tester.generateDependenceMatrix(lb8, ub8, wc8, rc8, d8);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_009DirectionVectorLT() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {5, 5, -5, -5, 2});
        rowsExpected.add(new double[] {-5, -5, 5, 5, -2});
        rowsExpected.add(new double[] {5, 5, -5, -5, 2});
        rowsExpected.add(new double[] {-5, -5, 5, 5, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 100});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 100});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 100});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 100});
        rowsExpected.add(new double[] {1, -1, 0, 0, -1});
        Matrix expected = new Matrix(rowsExpected);   
        Matrix result = tester.generateDependenceMatrix(lb9, ub9, wc9, rc9, d9);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_010DirectionVectorGT() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {5, 5, -5, -5, 2});
        rowsExpected.add(new double[] {-5, -5, 5, 5, -2});
        rowsExpected.add(new double[] {5, 5, -5, -5, 2});
        rowsExpected.add(new double[] {-5, -5, 5, 5, -2});
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 100});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 100});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 100});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 100});
        rowsExpected.add(new double[] {-1, 1, 0, 0, -1});
        Matrix expected = new Matrix(rowsExpected);  
        Matrix result = tester.generateDependenceMatrix(lb10, ub10, wc10, rc10, d10);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_011DirectionVectorEQ() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {5, 5, -5, -5, 2});
        rowsExpected.add(new double[] {-5, -5, 5, 5, -2});
        rowsExpected.add(new double[] {5, 5, -5, -5, 2});
        rowsExpected.add(new double[] {-5, -5, 5, 5, -2}); 
        rowsExpected.add(new double[] {-1, 0, 0, 0, -1});
        rowsExpected.add(new double[] {1, 0, 0, 0, 100});
        rowsExpected.add(new double[] {0, -1, 0, 0, -1});
        rowsExpected.add(new double[] {0, 1, 0, 0, 100});
        rowsExpected.add(new double[] {0, 0, -1, 0, -1});
        rowsExpected.add(new double[] {0, 0, 1, 0, 100});
        rowsExpected.add(new double[] {0, 0, 0, -1, -1});
        rowsExpected.add(new double[] {0, 0, 0, 1, 100});
        rowsExpected.add(new double[] {1, -1, 0, 0, 0});
        rowsExpected.add(new double[] {-1, 1, 0, 0, 0});
        Matrix expected = new Matrix(rowsExpected);
        Matrix result = tester.generateDependenceMatrix(lb11, ub11, wc11, rc11, d11);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
    
    @Test
    public void test100_012OneNestingLevel() {
        List<double[]> rowsExpected = new ArrayList<double[]>();
        rowsExpected.add(new double[] {5, -5, 2});
        rowsExpected.add(new double[] {-5, 5, -2});
        rowsExpected.add(new double[] {-1, 0, -1});
        rowsExpected.add(new double[] {1, 0, 100});
        rowsExpected.add(new double[] {0, -1, -1});
        rowsExpected.add(new double[] {0, 1, 100});
        Matrix expected = new Matrix(rowsExpected);
        Matrix result = tester.generateDependenceMatrix(lb12, ub12, wc12, rc12, d12);
        Assert.assertTrue("\nExpected:\n" + expected + "Got:\n" + result, expected.isEquivalentTo(result));
    }
}
