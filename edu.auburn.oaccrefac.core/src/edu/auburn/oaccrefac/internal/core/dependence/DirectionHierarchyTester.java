package edu.auburn.oaccrefac.internal.core.dependence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.auburn.oaccrefac.core.dependence.Direction;

/**
 * @author Alexander Calvert
 * 
 *         Class to encapsulate a for loop data dependence system
 *
 *         TODO: finish constructor (see other todo's for new fields, etc.) TODO: this class contains the inequality
 *         matrix for the loop system, but FMDepTest generates it from array info internally from loop bounds, etc.
 *         Either this class should keep loop bounds, etc. instead of the matrix, or FMDepTest should take the matrix as
 *         an argument rather than loop bounds, etc. This would, however, mess with the IDependenceTest interface.
 * 
 *         int[] lowerBounds, int[] upperBounds, int[][] writeCoefficients, int[][] readCoefficients
 * 
 *
 */
public class DirectionHierarchyTester {

    private FourierMotzkinDependenceTester fourierMotzkin = new FourierMotzkinDependenceTester();

    private int[] lowerBounds;
    private int[] upperBounds;
    private int[][] writeCoefficients;
    private int[][] readCoefficients;
    private int numScalars;

    public DirectionHierarchyTester(int[] lowerBounds, int[] upperBounds, int[][] writeCoefficients,
            int[][] readCoefficients, int numScalars) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
        this.writeCoefficients = writeCoefficients;
        this.readCoefficients = readCoefficients;
        this.numScalars = numScalars;
    }

    /**
     * TODO: get other arguments for the tester; probably also from constructor
     * 
     * @return a set containing all directions in which there might be a dependence
     */

    public Set<Direction[]> getPossibleDependenceDirections() {
        Direction[] allDirections = new Direction[lowerBounds.length];
        Arrays.fill(allDirections, Direction.ANY);

        Set<Direction[]> results = new HashSet<Direction[]>();

        return getPossibleDependenceDirections(allDirections, results);
    }

    private Set<Direction[]> getPossibleDependenceDirections(Direction[] dvEls, Set<Direction[]> results) {
        /*
         * run on given direction vector if there is no dependence, return list as-is otherwise add given direction
         * vector to the list get new direction vectors where all initial non-'*' elements are the same and the first
         * '*' is replaced with '<'/'='/'>' recursively run on each of three new direction vectors, adding the result to
         * the list
         * 
         */

        // if there is no dependence
        if (!fourierMotzkin.test(lowerBounds, upperBounds, writeCoefficients, readCoefficients, numScalars, dvEls)) {
            return results;
        } else {
            Direction[] originalVector = dvEls;
            int firstAny = Arrays.asList(originalVector).indexOf(Direction.ANY);

            // if we have a dependence, but this vector is at the
            // bottom of the hierarchy (no '*' element in the vector)
            if (firstAny < 0) {
                results.add(dvEls);
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
