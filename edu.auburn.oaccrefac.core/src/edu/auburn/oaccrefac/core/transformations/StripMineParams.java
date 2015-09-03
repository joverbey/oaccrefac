package edu.auburn.oaccrefac.core.transformations;

public class StripMineParams extends RefactoringParams {

    private int stripFactor;
    private int depth;

    public StripMineParams(int stripFactor, int depth) {
        this.stripFactor = stripFactor;
        this.depth = depth;
    }

    public int getStripFactor() {
        return stripFactor;
    }

    public int getDepth() {
        return depth;
    }

}