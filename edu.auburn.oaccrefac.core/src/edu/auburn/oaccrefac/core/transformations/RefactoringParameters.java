package edu.auburn.oaccrefac.core.transformations;

public abstract class RefactoringParameters {

    public static class InterchangeParams extends RefactoringParameters {
        private int depth;

        public InterchangeParams(int depth) {
            this.depth = depth;
        }

        public int getDepth() {
            return depth;
        }

    }

    public static class StripMineParams extends RefactoringParameters {

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

    public static class TileLoopsParams extends RefactoringParameters {
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

    public static class UnrollLoopParams extends RefactoringParameters {
        private int unrollFactor;

        public UnrollLoopParams(int unrollFactor) {
            this.unrollFactor = unrollFactor;
        }

        public int getUnrollFactor() {
            return unrollFactor;
        }

    }

}
