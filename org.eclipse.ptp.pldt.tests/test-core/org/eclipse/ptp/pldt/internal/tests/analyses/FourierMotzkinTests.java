/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Susan Chesnut - Initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.internal.tests.analyses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ptp.pldt.openacc.internal.core.dependence.FourierMotzkinEliminator;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.Matrix;

import junit.framework.TestCase;

/**
 * Unit tests for
 * {@link edu.auburn.oaccrefac.internal.core.dependence.photran.internal.core.analysis.dependence.FourierMotzkinEliminator}.
 * 
 * @author Susan Chesnut
 */
public class FourierMotzkinTests extends TestCase
{
    private Matrix matrixWithSolution;

    private Matrix noSolutionMatrix;

    private FourierMotzkinEliminator e;

    @Override
    public void setUp()
    {
        this.matrixWithSolution = new Matrix();
        this.e = new FourierMotzkinEliminator();
        double[] row1 = { 1, 1, 1, 10 };
        double[] row2 = { 1, -1, 2, 20 };
        double[] row3 = { 2, -1, -1, -1 };
        double[] row4 = { -1, 1, -1, 5 };
        matrixWithSolution.addRowAtIndex(0, row1);
        matrixWithSolution.addRowAtIndex(1, row2);
        matrixWithSolution.addRowAtIndex(2, row3);
        matrixWithSolution.addRowAtIndex(3, row4);
        matrixWithSolution.trimRowsToSize();

        this.noSolutionMatrix = new Matrix();
        double[] row5 = { 1, 0, 20 };
        double[] row6 = { -1, 0, -10 };
        double[] row7 = { 0, 1, 5 };
        double[] row8 = { 0, -1, 0 };
        double[] row9 = { 1, -1, 4 };
        noSolutionMatrix.addRowAtIndex(0, row5);
        noSolutionMatrix.addRowAtIndex(1, row6);
        noSolutionMatrix.addRowAtIndex(2, row7);
        noSolutionMatrix.addRowAtIndex(3, row8);
        noSolutionMatrix.addRowAtIndex(4, row9);
        noSolutionMatrix.trimRowsToSize();
    }

    // ex 4.23
    public void test100_010EliminateForRealSolution()
    {
        assertEquals(true, e.eliminateForRealSolutions(matrixWithSolution));
    }

    // ex 4.24
    public void test100_020EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, -4, 2 };
        double[] row2 = { 1, 5, 7 };
        double[] row3 = { -1, 0, -3 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    // ex 4.22
    public void test100_030EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 0, 0, 10 };
        double[] row2 = { -1, 0, 0, 0 };
        double[] row3 = { 1, -1, 0, 25 };
        double[] row4 = { 0, 1, 1, 15 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    // ex 4.5.1
    public void test100_040EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 0, 1, 6 };
        double[] row2 = { 1, 1, 9 };
        double[] row3 = { 1, -1, 5 };
        double[] row4 = { -2, -1, -7 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    // ex 4.25
    public void test100_050EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { -3, 2, 0 };
        double[] row2 = { 2, -3, 1 };
        double[] row3 = { 0, 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    public void test100_060EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 2, -1, 3 };
        double[] row2 = { 0, -1, 3 };
        double[] row3 = { -0.8, -1, -2.5 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    // ex 4.21
    public void test100_010EliminateForNoRealSolution()
    {
        assertEquals(false, e.eliminateForRealSolutions(noSolutionMatrix));
    }

    public void test100_020EliminateForNoRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, -1, -2 };
        double[] row2 = { -1, 1, -2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        assertEquals(false, e.eliminateForRealSolutions(m));
    }

    public void test100_900EliminateForNoRealSolutionFail()
    {
        Throwable thrown = null;
        Matrix m = new Matrix();
        try
        {
            e.eliminateForRealSolutions(m);
        }
        catch (IndexOutOfBoundsException realThrown)
        {
            thrown = realThrown;
        }
        assertTrue(thrown instanceof IndexOutOfBoundsException);
    }

    public void test200_010CalculateLowerBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(3);
        assertEquals(e.calculateLowerBoundSet(matrixWithSolution, 0), testSet);
    }

    public void test200_020CalculateLowerBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(1);
        testSet.add(2);
        assertEquals(e.calculateLowerBoundSet(matrixWithSolution, 1), testSet);
    }

    public void test200_030CalculateLowerBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(2);
        testSet.add(3);
        assertEquals(e.calculateLowerBoundSet(matrixWithSolution, 2), testSet);
    }

    public void test300_010CalculateUpperBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(0);
        testSet.add(1);
        testSet.add(2);
        assertEquals(e.calculateUpperBoundSet(matrixWithSolution, 0), testSet);
    }

    public void test300_020CalculateUpperBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(0);
        testSet.add(3);
        assertEquals(e.calculateUpperBoundSet(matrixWithSolution, 1), testSet);
    }

    public void test300_030CalculateUpperBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(0);
        testSet.add(1);
        assertEquals(e.calculateUpperBoundSet(matrixWithSolution, 2), testSet);
    }

//    public void test400_010SortMatrixBoundingSetsAtTop()
//    {
//        Matrix m = new Matrix();
//        List<Integer> temp1 = new ArrayList<Integer>();
//        List<Integer> temp2 = new ArrayList<Integer>();
//        temp1.add(1);
//        temp2.add(2);
//        m = e.sortMatrixByBoundingSetContents(noSolutionMatrix, temp1, temp2);
//        assertEquals(m.getValueAtMatrixIndex(0, 0), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 2), 5.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 0), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 1), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 2), -10.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 1), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 2), 20.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 0), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 2), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 2), 4.0);
//    }

//    public void test400_020SortMatrixBoundingSetsAtTop()
//    {
//        Matrix m = new Matrix();
//        List<Integer> temp1 = new ArrayList<Integer>();
//        List<Integer> temp2 = new ArrayList<Integer>();
//        temp1.add(0);
//        temp2.add(2);
//        temp2.add(4);
//        m = e.sortMatrixByBoundingSetContents(noSolutionMatrix, temp1, temp2);
//        assertEquals(m.getValueAtMatrixIndex(0, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 2), 4.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 0), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 1), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 2), 5.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 1), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 2), 20.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 0), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 1), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 2), -10.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 0), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 2), 0.0);
//    }

//    public void test400_030SortMatrixBoundingSetsAtTop()
//    {
//        Matrix m = new Matrix();
//        List<Integer> low = new ArrayList<Integer>();
//        List<Integer> up = new ArrayList<Integer>();
//        low.add(2);
//        low.add(3);
//        up.add(0);
//        up.add(1);
//        m = e.sortMatrixByBoundingSetContents(matrixWithSolution, low, up);
//        assertEquals(m.getValueAtMatrixIndex(0, 0), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 2), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 0), 2.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 2), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 2), 2.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 1), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 2), 1.0);
//    }

//    public void test400_040SortMatrixBoundingSetsAtTop()
//    {
//        Matrix m = new Matrix();
//        List<Integer> low = new ArrayList<Integer>();
//        List<Integer> up = new ArrayList<Integer>();
//        low.add(2);
//        low.add(3);
//        up.add(0);
//        up.add(1);
//        double[] newRow = { 9, 9, 9, 9 };
//        matrixWithSolution.addRowAtIndex(4, newRow);
//        m = e.sortMatrixByBoundingSetContents(matrixWithSolution, low, up);
//        assertEquals(m.getValueAtMatrixIndex(0, 0), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 2), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 0), 2.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 2), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 2), 2.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 1), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(3, 2), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 0), 9.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 1), 9.0);
//        assertEquals(m.getValueAtMatrixIndex(4, 2), 9.0);
//    }

//    public void test500_010DeleteFromSortedMatrix()
//    {
//        Matrix m = new Matrix();
//        m = e.deleteRowsFromSortedMatrix(noSolutionMatrix, 2);
//        assertEquals(m.getNumRows(), 3);
//        assertEquals(m.getValueAtMatrixIndex(0, 0), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 2), 5.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 0), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(1, 2), 0.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(2, 2), 4.0);
//    }

//    public void test500_020DeleteFromSortedMatrix()
//    {
//        Matrix m = new Matrix();
//        m = e.deleteRowsFromSortedMatrix(noSolutionMatrix, 4);
//        assertEquals(m.getNumRows(), 1);
//        assertEquals(m.getValueAtMatrixIndex(0, 0), 1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 1), -1.0);
//        assertEquals(m.getValueAtMatrixIndex(0, 2), 4.0);
//    }

//    public void test500_030DeleteFromSortedMatrix()
//    {
//        Matrix m = new Matrix();
//        m = e.deleteRowsFromSortedMatrix(noSolutionMatrix, 5);
//        assertEquals(m.getNumRows(), 0);
//    }

    public void test600_010DeleteAllUnconstrainedVariables()
    {
        Matrix m = new Matrix();
        double[] row1 = { 0, 1, 2, 3, 20 };
        double[] row2 = { 0, -1, 2, 0, 30 };
        double[] row3 = { 1, 0, 0, 1, 40 };
        double[] row4 = { 2, 1, 3, 1, 50 };
        double[] row5 = { -1, 1, 1, 1, 60 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        m = e.deleteAllUnconstrainedVariables(m);
        assertEquals(0, m.getNumRows());
    }

    // ex 4.23
    public void test1010_010RealProjection()
    {
        Matrix solutionSpace = new Matrix();
        double[] new1 = { 3.0, 0.0, 0.0, 9.0 };
        double[] new2 = { 2.5, -1.5, 0.0, 9.0 };
        double[] new3 = { 0.0, 2.0, 0.0, 15.0 };
        double[] new4 = { -0.5, 0.5, 0.0, 15.0 };
        solutionSpace.addRowAtIndex(0, new1);
        solutionSpace.addRowAtIndex(1, new2);
        solutionSpace.addRowAtIndex(2, new3);
        solutionSpace.addRowAtIndex(3, new4);
        assertEquals(solutionSpace.toString(), e.realProjection(matrixWithSolution, 2).toString());
    }

    public void test1030_010DeterminelUnconstrainedVariables()
    {
        Matrix m = new Matrix();
        double[] row1 = { 0, 1, 2, 3, 20 };
        double[] row2 = { 0, -1, 2, 0, 30 };
        double[] row3 = { 1, 0, 0, 1, 40 };
        double[] row4 = { 2, 1, 3, 1, 50 };
        double[] row5 = { -1, 1, 1, 1, 60 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        boolean[] sol = { false, false, true, true };
        assertTrue(Arrays.equals(sol, e.determineUnconstrainedVariables(m)));
    }

    public void test1040_010DeleteUnconstrainedVariable()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 0, 20 };
        double[] row6 = { -1, 0, -10 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        assertEquals(m.toString(), e.deleteUnconstrainedVariable(noSolutionMatrix, 1).toString());
    }

    public void testCombineBoundingSets()
    {
        List<Integer> upper = new ArrayList<Integer>();
        List<Integer> lower = new ArrayList<Integer>();
        upper.add(1);
        upper.add(6);
        lower.add(2);
        List<Integer> combined = new ArrayList<Integer>();
        combined.add(1);
        combined.add(2);
        combined.add(6);
        assertTrue(combined.equals(e.combineBoundingSets(upper, lower)));
    }

    public void testDeleteRowsInBoundingSet()
    {
        Matrix m = new Matrix();
        List<Integer> combined = new ArrayList<Integer>();
        combined.add(0);
        combined.add(1);
        combined.add(3);
        m = e.deleteRowsInBoundingSet(matrixWithSolution, combined);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 2.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), -1.0);
    }

    public void testDeleteRowsOfAllZeroes()
    {
        Matrix m = new Matrix();
        double[] row1 = { -3, 2, 0 };
        double[] row2 = { 0, 0, 0 };
        double[] row3 = { 0, 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m = e.deleteRowsOfAllZeroes(m);
        assertEquals(2, m.getNumRows());
        assertEquals(m.getValueAtMatrixIndex(0, 0), -3.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 2.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 2.0);
    }

    public void testIsInconsistent()
    {
        double[] ar1 = { 1, 2, 3, 4 };
        double[] ar2 = { -1, -2, -3, -5 };
        assertEquals(true, e.isInconsistentInequality(ar1, ar2));
    }

    public void testIsInconsistent2()
    {
        double[] ar1 = { 3, -1, 3, -6 };
        double[] ar2 = { -3, 1, -3, -5 };
        assertEquals(true, e.isInconsistentInequality(ar1, ar2));
    }

    public void testIsInconsistent3()
    {
        double[] ar1 = { 1, 2, 3, 4 };
        double[] ar2 = { -1, -2, -3, -4 };
        assertEquals(false, e.isInconsistentInequality(ar1, ar2));
    }

    public void testIsInconsistent4()
    {
        double[] ar1 = { 1, 2, 3, 4 };
        double[] ar2 = { -3, -1, -3, -5 };
        assertEquals(false, e.isInconsistentInequality(ar1, ar2));
    }

    public void test1070_010ContainsInconsistentInequalities()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { -1, -2, -3, -5 };
        double[] row3 = { 1, 0, 0, 1 };
        double[] row4 = { 2, 1, 3, 1 };
        double[] row5 = { 1, 0, 0, 1 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        assertEquals(true, e.containsInconsistentInequalities(m));
    }

    public void test1070_020ContainsInconsistentInequalities()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { -1, -2, -3, -4 };
        double[] row3 = { 1, 0, 0, 1 };
        double[] row4 = { 2, 1, 3, 1 };
        double[] row5 = { 1, 0, 0, 1 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        assertEquals(false, e.containsInconsistentInequalities(m));
    }
}
