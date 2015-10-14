package edu.auburn.oaccrefac.core.transformations;

public class LoopCuttingParams extends RefactoringParams {
    private int cutFactor;
    
    public LoopCuttingParams(int stripFactor) {
        this.cutFactor = stripFactor;
    }

    public int getCutFactor() {
        return cutFactor;
    }
}
