package edu.auburn.oaccrefac.core.transformations;

public class StripMineParams extends RefactoringParams {

    private int stripFactor;

    public StripMineParams(int stripFactor) {
        this.stripFactor = stripFactor;
    }

    public int getStripFactor() {
        return stripFactor;
    }

}