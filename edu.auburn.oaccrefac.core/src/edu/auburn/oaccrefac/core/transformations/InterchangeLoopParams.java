package edu.auburn.oaccrefac.core.transformations;

public class InterchangeLoopParams extends RefactoringParams {
    private int depth;

    public InterchangeLoopParams(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

}