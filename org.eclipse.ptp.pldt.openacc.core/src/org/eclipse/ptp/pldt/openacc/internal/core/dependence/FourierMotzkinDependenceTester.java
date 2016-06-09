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
package org.eclipse.ptp.pldt.openacc.internal.core.dependence;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ptp.pldt.openacc.core.dependence.Direction;

public class FourierMotzkinDependenceTester {

    /**
     * assumes that the subscript expressions are normalized see example on Wolfe pg 225-226
     * 
     * coefficients are arrays of subscript linear expressions a linear expression should be an array of doubles,
     * listing the constant value first, then the induction var coefficients in order, then the scalar coefficients in
     * order ie, 1 + 2i_2 + 3N + 4i_1 becomes [1 4 2 3]
     * 
     * writeCoefficients constant first, then induction var coeffs, then scalar coeffs
     * readCoefficients constant first, then induction var coeffs, then scalar coeffs
     */
    
    public boolean test(int[] lowerBounds, int[] upperBounds, int[][] writeCoefficients, int[][] readCoefficients,
            int numScalars, Direction[] direction) {

        // if there are only constant subscripts given, if those values are equal, there is a dependence

        // check if there is any subscript of the array where the read and the write are different constants
        // ie, arr[2i+1][4] = arr[i+3][6] : 4 != 6, so no dependence can exist
        boolean allSubscriptsAreConstants = true;
        for (int subscript = 0; subscript < writeCoefficients.length; subscript++) {
            boolean allWriteCoeffsExceptConstantAreZero = true;
            boolean allReadCoeffsExceptConstantAreZero = true;
            for (int coeff = 1; coeff < writeCoefficients[subscript].length; coeff++) {
                if (writeCoefficients[subscript][coeff] != 0) {
                    allWriteCoeffsExceptConstantAreZero = false;
                    break;
                }
            }
            for (int coeff = 1; coeff < readCoefficients[subscript].length; coeff++) {
                if (readCoefficients[subscript][coeff] != 0) {
                    allReadCoeffsExceptConstantAreZero = false;
                    break;
                }
            }
            if (allWriteCoeffsExceptConstantAreZero && allReadCoeffsExceptConstantAreZero) {
                if (writeCoefficients[subscript][0] != readCoefficients[subscript][0]) {
                    return false;
                }
            } else {
                allSubscriptsAreConstants = false;
            }
        }
        if (allSubscriptsAreConstants) {
            return true;
        }

        FourierMotzkinEliminator el = new FourierMotzkinEliminator();
        
        Matrix m = generateDependenceMatrix(lowerBounds, upperBounds, writeCoefficients, readCoefficients, numScalars,
                direction);
        

        // if there is an integer solution to the matrix, there is (possibly) a
        // dependence; otherwise, there is no dependence
        // TODO: This is obviously a workaround. See the fixme in FMEliminator.
        try {
            return el.eliminateForIntegerSolutions(m);
        	//return el.eliminateForRealSolutions(m);
        } catch (IndexOutOfBoundsException e) {
            System.err.print("FourierMotzkinEliminator#eliminateForIntegerSolutions ");
            System.err.println("threw an IndexOutOfBoundsException.");
            System.err.println("Conservatively assuming a dependence exists...");
            return true;
        }
    }

    private double[] listToPrimitiveArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private double[] negate(double[] arr) {
        double[] ret = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = -arr[i];
        }
        return ret;
    }

    public Matrix generateDependenceMatrix(int[] lowerBounds, int[] upperBounds, int[][] writeCoefficients,
            int[][] readCoefficients, int numScalars, Direction[] direction) {
        
        Matrix m = new Matrix();
        // get the inequalities from the subscripts
        for (int i = 0; i < writeCoefficients.length; i++) {
            // get coefficients from induction vars
            ArrayList<Double> row = new ArrayList<Double>();
            for (int j = 1; j < writeCoefficients[i].length - numScalars; j++) {
                row.add((double) writeCoefficients[i][j]);
            }
            for (int j = 1; j < readCoefficients[i].length - numScalars; j++) {
                row.add((double) -readCoefficients[i][j]);
            }
            // get coefficients from scalars
            for (int j = writeCoefficients[i].length - numScalars; j < writeCoefficients[i].length; j++) {
                row.add((double) (writeCoefficients[i][j] - readCoefficients[i][j]));
            }
            // get coefficients from constants
            row.add((double) (readCoefficients[i][0] - writeCoefficients[i][0]));
            m.addRowAtIndex(m.getNumRows(), listToPrimitiveArray(row));
            m.addRowAtIndex(m.getNumRows(), negate(listToPrimitiveArray(row)));
        }

        // get the inequalities from the loop bounds
        // -i_d1 <= -l1; -1 0 0 ... -l1
        // i_d1 <= u1; 1 0 0 ... u1
        // -i_u1 <= -l1; 0 -1 0 ... -l1
        // i_u1 <= u1; 0 1 0 ... u1
        // ...
        for (int i = 0; i < lowerBounds.length; i++) {
            double[] row1 = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
            double[] row2 = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
            double[] row3 = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
            double[] row4 = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
            row1[i] = -1;
            row1[row1.length - 1] = -lowerBounds[i];
            row2[i] = 1;
            row2[row2.length - 1] = upperBounds[i];
            row3[i + ((row3.length-1-numScalars)/2)] = -1;
            row3[row3.length - 1] = -lowerBounds[i];
            row4[i + ((row4.length-1-numScalars)/2)] = 1;
            row4[row4.length - 1] = upperBounds[i];
            m.addRowAtIndex(m.getNumRows(), row1);
            m.addRowAtIndex(m.getNumRows(), row2);
            m.addRowAtIndex(m.getNumRows(), row3);
            m.addRowAtIndex(m.getNumRows(), row4);
        }

        // get the inequalities from the dependence direction vector
        // iterate through the direction vector, adding an inequality
        // comparing i_d and i_u for that particular vector element
        for (int i = 0; i < direction.length; i++) {
            double[] row;
            // all elements default to zero
            switch (direction[i]) {
            case ANY:
                // don't add any more constraints
                break;
            case EQ:
                // i_d - i_u <= 0, -i_d + i_u <= 0
                row = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
                row[i] = 1;
                row[i + ((row.length-1-numScalars)/2)] = -1;
                row[row.length - 1] = 0;
                m.addRowAtIndex(m.getNumRows(), row);
                row = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
                row[i] = -1;
                row[i + ((row.length-1-numScalars)/2)] = 1;
                row[row.length - 1] = 0;
                m.addRowAtIndex(m.getNumRows(), row);
                break;
            case GT:
                // i_u < i_d
                // in integer terms, i_u <= i_d-1, or -i_d+i_u <= -1
                row = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
                row[i] = -1;
                row[i + ((row.length-1-numScalars)/2)] = 1;
                row[row.length - 1] = -1;
                m.addRowAtIndex(m.getNumRows(), row);
                break;
            case LT:
                // i_d < i_u
                // in integer terms, i_d <= i_u-1, or i_d-i_u <= -1
                row = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
                row[i] = 1;
                row[i + ((row.length-1-numScalars)/2)] = -1;
                row[row.length - 1] = -1;
                m.addRowAtIndex(m.getNumRows(), row);
                break;
            case GE:
                // i_u <= i_d
                // ie, -i_d+i_u <= 0
                row = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
                row[i] = -1;
                row[i + ((row.length-1-numScalars)/2)] = 1;
                row[row.length - 1] = 0;
                m.addRowAtIndex(m.getNumRows(), row);
                break;
            case LE:
                // i_d <= i_u
                // ie, i_d-i_u <= 0
                row = new double[writeCoefficients[0].length + readCoefficients[0].length - numScalars - 1];
                row[i] = 1;
                row[i + ((row.length-1-numScalars)/2)] = -1;
                row[row.length - 1] = 0;
                m.addRowAtIndex(m.getNumRows(), row);
                break;
            default:
                throw new UnsupportedOperationException();
            }
        }
        return m;
    }

}
