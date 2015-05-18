package edu.auburn.oaccrefac.internal.core;
public class InductionVariable {
    private final String name;

    private int lowerBound = -1;
    private int upperBound = -1;

    public InductionVariable(String name, int firstBound, int secondBound) {
        this.name = name;
        lowerBound = Math.min(firstBound, secondBound);
        upperBound = Math.max(firstBound, secondBound);
    }

    public int getTripCount() {
        return upperBound - lowerBound;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return String.format("InductionVariable<%s, %d, %d, %d>", name, lowerBound, upperBound, getTripCount());
    }
}
