package edu.auburn.oaccrefac.core.transformations;

public class TileLoopsParams extends RefactoringParams {
    private int depth;
    private int stripFactor;
    private int propagate;

    public TileLoopsParams(int depth, int stripFactor, int propagate) {
        this.depth = depth;
        this.stripFactor = stripFactor;
        this.propagate = propagate;
    }

    public int getDepth() {
        return depth;
    }

    public int getStripFactor() {
        return stripFactor;
    }

    public int getPropagate() {
        return propagate;
    }

}