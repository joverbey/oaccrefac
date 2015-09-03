package edu.auburn.oaccrefac.core.transformations;

public class UnrollLoopParams extends RefactoringParams {
    private int unrollFactor;

    public UnrollLoopParams(int unrollFactor) {
        this.unrollFactor = unrollFactor;
    }

    public int getUnrollFactor() {
        return unrollFactor;
    }

}