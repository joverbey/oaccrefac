/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Susan Chesnut - Initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.dependence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of Fourier-Motzkin elimination with Pugh's dark shadow improvement.
 * 
 * @author Susan Chesnut
 * @edit October 15, 2014 Initial API and implementation
 * 
 */
/*
 * We assume that every inequality in the matrix is of the form (Ax <= b) Determines whether a real
 * solution to a system of linear inequalities exists. Solves the system by projecting it onto a
 * reduced number of unknowns, eliminating one unknown at a time.
 */
public class FourierMotzkinEliminator {

	static final double EPSILON = 0.000000001;

	/**
	 * The Fourier-Motzkin Elimination algorithm
	 * 
	 * @param matrixIn
	 *            system of linear inequalities on which to find whether a real solution exists
	 *            or not
	 * @return whether a solution exists or not
	 */
	public boolean eliminateForRealSolutions(Matrix matrixIn) {
		if (matrixIn.getNumRows() == 0)
			throw new IndexOutOfBoundsException("eliminateForRealSolutions - matrixIn is empty"); //$NON-NLS-1$

		Matrix unconstrainedMatrix = new Matrix();
		while (unconstrainedMatrix.getNumRows() != matrixIn.getNumRows()) {
			unconstrainedMatrix = matrixIn.cloneMatrix();
			matrixIn = deleteAllUnconstrainedVariables(matrixIn);
			if (matrixIn.getNumRows() == 0)
				return true;
		}

		unconstrainedMatrix = null;

		matrixIn = deleteAllUnconstrainedVariables(matrixIn);
		matrixIn = deleteRowsOfAllZeroes(matrixIn);
		if (matrixIn.getNumRows() == 0)
			return true;
		if (containsInconsistentInequalities(matrixIn))
			return false;

		// if 4 columns, eliminatedVar is 3 (subtract 1)
		// but Matrix index starts at 0, so subtract 2 to get correct index
		int eliminatedVar = matrixIn.getNumColumns() - 2;

		// Continue until we have eliminated every variable
		// or we have eliminated every row
		while (eliminatedVar >= 0 && matrixIn.getNumRows() > 0) {
			matrixIn = realProjection(matrixIn, eliminatedVar);
			matrixIn = deleteRowsOfAllZeroes(matrixIn);
			if (containsInconsistentInequalities(matrixIn))
				return false;
			eliminatedVar--;
		}

		// all variables have been eliminated - a solution exists
		if (matrixIn.getNumRows() == 0) {
			return true;
		}

		// simple inequalities exist - all coefficients should be 0
		else {
			int numColumns = matrixIn.getNumColumns() - 1;
			for (int i = 0; i < matrixIn.getNumRows(); i++) {
				// for all rows left, if the last column is less than 0
				// there cannot be a solution since the matrix is in the form
				// Ax <= b ... b must be 0 or greater
				if (matrixIn.getValueAtMatrixIndex(i, numColumns) < 0) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * @param matrixIn
	 *            the matrix being projected on
	 * @param eliminatingVar
	 *            the variable being eliminated
	 * @return the matrix produced after doing a single real projection on matrixIn
	 */
	public Matrix realProjection(Matrix matrixIn, int eliminatingVar) {
		// obtain lower and upper bounding sets - may not encompass all rows
		// sets contain the row numbers of the matrix
		List<Integer> lowerBoundSet = calculateLowerBoundSet(matrixIn, eliminatingVar);
		List<Integer> upperBoundSet = calculateUpperBoundSet(matrixIn, eliminatingVar);

		// if either bounding set is empty, delete anything with the variable
		// being eliminated and start eliminating a new variable
		// i.e., this variable is unconstrained so eliminate another
		if (lowerBoundSet.isEmpty() || upperBoundSet.isEmpty()) {
			matrixIn = deleteRowsInBoundingSet(matrixIn,
					combineBoundingSets(lowerBoundSet, upperBoundSet));
			return matrixIn;
		}

		// for all rows in the bounding sets, divide the row by
		// the absolute value of that row's eliminating variable's value
		// i.e. divide row A_i by |a_ij|
		for (int i = 0; i < lowerBoundSet.size(); i++) {
			matrixIn.divideRowByColIndex(lowerBoundSet.get(i), eliminatingVar);
		}

		for (int i = 0; i < upperBoundSet.size(); i++) {
			matrixIn.divideRowByColIndex(upperBoundSet.get(i), eliminatingVar);
		}

		// compare bounds to derive a new inequality that no longer
		// contains the variable being eliminated - (this variable should become 0.0)
		for (int i = 0; i < lowerBoundSet.size(); i++) {
			for (int k = 0; k < upperBoundSet.size(); k++) {
				double[] newRow = matrixIn.addTwoRowsToCreateNewRow(
						matrixIn.getRow(lowerBoundSet.get(i)),
						matrixIn.getRow(upperBoundSet.get(k)));
				matrixIn.addRowAtIndex(matrixIn.getNumRows(), newRow);

			}
		}

		// delete rows contained in the bounding sets
		// all inequalities involving eliminatedVar are deleted
		matrixIn = deleteRowsInBoundingSet(matrixIn,
				combineBoundingSets(lowerBoundSet, upperBoundSet));
		matrixIn.trimRowsToSize(); // helps when growth of inequalities tends toward exponential
		return matrixIn;
	}

	/**
	 * @param matrixIn
	 * @return whether the matrix contains inconsistent inequalities or not inconsistent meaning Aj
	 *         <= bj and Ai <= bk where Aj = -Ak and -bk > bj
	 */
	public boolean containsInconsistentInequalities(Matrix matrixIn) {
		boolean inconsistent = false;
		for (int i = 0; i < matrixIn.getNumRows(); i++) {
			for (int k = i + 1; k < matrixIn.getNumRows(); k++) {
				if (isInconsistentInequality(matrixIn.getRow(i),
						matrixIn.getRow(k))) {
					return true;
				}
			}
		}
		return inconsistent;
	}

	/**
	 * @param row1
	 *            Aj - the first row to compare
	 * @param row2
	 *            Ak - the second row to compare
	 * @return whether the two arrays create an inconsistency inconsistent meaning Aj <= bj and Ai
	 *         <= bk where Aj = -Ak and -bk > bj
	 */
	public boolean isInconsistentInequality(double[] row1, double[] row2) {
		for (int i = 0; i < row1.length - 1; i++) {
			if (row1[i] != -row2[i])
				return false;
		}
		// if it gets here, Aj = -Ak, check if -bk > bj
		if (-row2[row1.length - 1] > row1[row1.length - 1])
			return true;
		else
			return false;
	}

	/**
	 * @param matrixIn
	 *            matrix to be changed
	 * @return matrixIn minus any unconstrained variables
	 */
	public Matrix deleteAllUnconstrainedVariables(Matrix matrixIn) {
		if (matrixIn.getNumRows() == 0)
			throw new IndexOutOfBoundsException(
					"deleteAllUnconstrainedVariables - matrixIn is empty"); //$NON-NLS-1$

		boolean[] constrainedVars = determineUnconstrainedVariables(matrixIn);
		for (int i = 0; i < constrainedVars.length; i++) {
			if (constrainedVars[i]) {
				matrixIn = deleteUnconstrainedVariable(matrixIn, i);
			}
		}
		matrixIn.trimRowsToSize();
		return matrixIn;
	}

	/**
	 * @param matrixIn
	 * @param colIndex
	 *            the variable we are eliminating from matrixIn
	 * @return a list of integers that contain the row indices for a lower bounding set L = { i |
	 *         a_ij < 0 }
	 */
	public List<Integer> calculateLowerBoundSet(Matrix matrixIn, int colIndex) {
		if (matrixIn.getNumRows() == 0)
			throw new IndexOutOfBoundsException("calculateLowerBoundSet - matrixIn is empty"); //$NON-NLS-1$

		if (colIndex < 0 || colIndex >= matrixIn.getNumColumns())
			throw new IndexOutOfBoundsException("calculateLowerBoundSet - colIndex out of bounds"); //$NON-NLS-1$

		List<Integer> lowerBound = new ArrayList<Integer>();
		for (int i = 0; i < matrixIn.getNumRows(); i++) {
			if (matrixIn.getValueAtMatrixIndex(i, colIndex) < 0) {
				lowerBound.add(i);
			}
		}
		return lowerBound;
	}

	/**
	 * colIndex is the variable we are eliminating from matrixIn
	 * 
	 * @param matrixIn
	 * @param colIndex
	 * @return a list of integers that contain the row indices for an upper bounding set U = { i |
	 *         a_ij > 0 }
	 */
	public List<Integer> calculateUpperBoundSet(Matrix matrixIn, int colIndex) {
		if (matrixIn.getNumRows() == 0)
			throw new IndexOutOfBoundsException("calculateUpperBoundSet - matrixIn is empty"); //$NON-NLS-1$

		if (colIndex < 0 || colIndex >= matrixIn.getNumColumns())
			throw new IndexOutOfBoundsException("calculateUpperBoundSet - colIndex out of bounds"); //$NON-NLS-1$

		List<Integer> upperBound = new ArrayList<Integer>();
		for (int i = 0; i < matrixIn.getNumRows(); i++) {
			if (matrixIn.getValueAtMatrixIndex(i, colIndex) > 0) {
				upperBound.add(i);
			}
		}
		return upperBound;
	}

	/**
	 * @param matrixIn
	 *            matrix to be changed
	 * @return unconstrained an array describing whether a variable is constrained
	 */
	public boolean[] determineUnconstrainedVariables(Matrix matrixIn) {
		if (matrixIn.getNumRows() == 0)
			return null;

		int numOfVariables = matrixIn.getNumColumns() - 1;
		boolean[] unconstrained = new boolean[numOfVariables];

		for (int i = 0; i < numOfVariables; i++) {
			if (calculateLowerBoundSet(matrixIn, i).isEmpty()
					|| calculateUpperBoundSet(matrixIn, i).isEmpty())
				unconstrained[i] = true;
		}
		return unconstrained;
	}

	/**
	 * @param matrixIn
	 *            matrix to be changed
	 * @param unconstrainedIndex
	 *            the column index of an unconstrained variable
	 * @return matrixIn minus any row containing a non-zero value in the column "unconstrainedIndex"
	 */
	public Matrix deleteUnconstrainedVariable(Matrix matrixIn, int unconstrainedIndex) {
		if (matrixIn.getNumRows() == 0)
			return matrixIn;

		if (unconstrainedIndex < 0 || unconstrainedIndex >= matrixIn.getNumColumns())
			throw new IndexOutOfBoundsException(
					"deleteUnconstrainedVariable - unconstrainedIndex out of bounds"); //$NON-NLS-1$

		List<Integer> lowerBoundSet = null;
		List<Integer> upperBoundSet = null;

		lowerBoundSet = calculateLowerBoundSet(matrixIn, unconstrainedIndex);
		upperBoundSet = calculateUpperBoundSet(matrixIn, unconstrainedIndex);

		matrixIn = deleteRowsInBoundingSet(matrixIn,
				combineBoundingSets(lowerBoundSet, upperBoundSet));

		return matrixIn;
	}

	/**
	 * @param matrixIn
	 * @param combinedSet
	 *            the combined set of the lowerbound and upperbound sets (comes from
	 *            combineBoundingSets)
	 * @return matrixIn minus the rows with row indexes in combinedSet
	 */
	public Matrix deleteRowsInBoundingSet(Matrix matrixIn, List<Integer> combinedSet) {
		if (matrixIn.getNumRows() == 0)
			throw new IndexOutOfBoundsException("deleteRowsInBoundingSet - matrixIn is empty"); //$NON-NLS-1$

		int numDeleted = 0;
		for (int i = 0; i < combinedSet.size(); i++) {
			matrixIn.deleteRow(combinedSet.get(i) - numDeleted);
			numDeleted++;
		}
		return matrixIn;
	}

	/**
	 * @param matrixIn
	 * @return matrixIn minus any rows that were full of zeroes
	 */
	public Matrix deleteRowsOfAllZeroes(Matrix matrixIn) {
		int numDeleted = 0;
		for (int i = 0; i < matrixIn.getNumRows(); i++) {
			if (matrixIn.isRowFullOfZeroes(matrixIn.getRow(i))) {
				matrixIn.deleteRow(i - numDeleted);
				numDeleted++;
			}
		}
		return matrixIn;
	}

	/**
	 * Combines the values in two List<Integer> objects
	 * 
	 * @param lowerBoundSet
	 * @param upperBoundSet
	 * @return a List containing the values from both lowerBoundSet and uppserBoundSet in sorted
	 *         order
	 */
	public List<Integer> combineBoundingSets(List<Integer> lowerBoundSet,
			List<Integer> upperBoundSet) {
		List<Integer> combinedList = new ArrayList<Integer>();
		for (int i = 0; i < lowerBoundSet.size(); i++) {
			combinedList.add(lowerBoundSet.get(i));
		}

		for (int i = 0; i < upperBoundSet.size(); i++) {
			combinedList.add(upperBoundSet.get(i));
		}
		Collections.sort(combinedList);
		return combinedList;
	}

}