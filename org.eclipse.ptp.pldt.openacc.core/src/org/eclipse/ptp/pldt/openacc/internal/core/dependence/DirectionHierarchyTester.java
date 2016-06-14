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
package org.eclipse.ptp.pldt.openacc.internal.core.dependence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.ptp.pldt.openacc.core.dependence.Direction;

public class DirectionHierarchyTester {

    private FourierMotzkinDependenceTester fourierMotzkin = new FourierMotzkinDependenceTester();

    private int[] lowerBounds;
    private int[] upperBounds;
    private int[][] writeCoefficients;
    private int[][] readCoefficients;
    private int numScalars;

    /**
     * @param lowerBounds 
     * @param upperBounds 
     * @param writeCoefficients constant first, then induction var coeffs, then scalar coeffs 
     * @param readCoefficients constant first, then induction var coeffs, then scalar coeffs
     * @param numScalars 
     */
    public DirectionHierarchyTester(int[] lowerBounds, int[] upperBounds, int[][] writeCoefficients,
            int[][] readCoefficients, int numScalars) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
        this.writeCoefficients = writeCoefficients;
        this.readCoefficients = readCoefficients;
        this.numScalars = numScalars;
    }

    /**
     * 
     * Uses the direction vector hierarchy to find all possible direction vectors
     * for a loop dependence
     * 
     * 
     * @return a set containing all directions in which there might be a dependence
     */
    
    public Set<Direction[]> getPossibleDependenceDirections() {
        Direction[] allDirections = new Direction[lowerBounds.length];
        Arrays.fill(allDirections, Direction.ANY);

        Set<Direction[]> results = new HashSet<Direction[]>();

        
        Set<Direction[]> allVectors = getPossibleDependenceDirections(allDirections, results);
        
        Iterator<Direction[]> iter = allVectors.iterator();
        while(iter.hasNext()) {
loop:       for(Direction d : iter.next()) {
                switch (d) {
                    case EQ:
                        continue;
                    case LT:
                    case LE:
                    case ANY:
                        break loop;
                    case GT:
                    case GE:
                        iter.remove();
                        break loop;
                    default:
                        throw new IllegalStateException();
                }
            }
        }
        return allVectors;

    }
    private Set<Direction[]> getPossibleDependenceDirections(Direction[] dv, Set<Direction[]> results) {
        /*
         * run on given direction vector if there is no dependence, return list as-is otherwise add given direction
         * vector to the list get new direction vectors where all initial non-'*' elements are the same and the first
         * '*' is replaced with '<'/'='/'>' recursively run on each of three new direction vectors, adding the result to
         * the list
         * 
         */

        // if there is no dependence
        if (!fourierMotzkin.test(lowerBounds, upperBounds, writeCoefficients, readCoefficients, numScalars, dv)) {
            return results;
        } else {
            Direction[] originalVector = dv;
            int firstAny = Arrays.asList(originalVector).indexOf(Direction.ANY);

            // if we have a dependence, but this vector is at the
            // bottom of the hierarchy (no '*' element in the vector)
            if (firstAny < 0) {
                results.add(dv);
                return results;
            } else {
                Direction[] newLT = Arrays.copyOf(originalVector, originalVector.length);
                Direction[] newGT = Arrays.copyOf(originalVector, originalVector.length);
                Direction[] newEQ = Arrays.copyOf(originalVector, originalVector.length);
                newLT[firstAny] = Direction.LT;
                newGT[firstAny] = Direction.GT;
                newEQ[firstAny] = Direction.EQ;

                results.addAll(getPossibleDependenceDirections(newLT, results));
                results.addAll(getPossibleDependenceDirections(newGT, results));
                results.addAll(getPossibleDependenceDirections(newEQ, results));

                return results;
            }
        }
    }
}
