/*
 * Runner class for all other refactorings from command line.
 */
public class Refactor {
    
    public static void main(String[] args) { 
        new Refactor().run(args);
    }
    
    public void run(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Refactor <refactoring> <args>");
            System.err.println();
            System.err.println("Available refactorings are:");
            System.err.println("DistributeLoops"); // Complete
            System.err.println("FuseLoops"); // Complete
            System.err.println("InterchangeLoops"); // Complete
            System.err.println("IntroduceDefaultNone:");
            System.err.println("IntroduceKernelsLoop"); // Complete
            System.err.println("IntroduceParallelLoop"); // Complete
            System.err.println("LoopCutting"); // Complete
            System.err.println("StripMine");
            System.err.println("TileLoops");
            System.err.println("Unroll"); // Complete
        } else {
            String[] refactoringArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                refactoringArgs[i-1] = args[i];
            }
            switch (args[0]) {
            case "DistributeLoops":
                new DistributeLoops().run(refactoringArgs);
                break;
            case "FuseLoops":
                new FuseLoops().run(refactoringArgs);
                break;
            case "InterchangeLoops":
                new InterchangeLoops().run(refactoringArgs);
                break;
            case "IntroduceDefaultNone":
                // new IntroduceDefaultNone(); Has different constructor
                break;
            case "IntroduceKernelsLoop":
                new IntroduceKernelsLoop().run(refactoringArgs);
                break;
            case "IntroduceParallelLoop":
                new IntroduceParallelLoop().run(refactoringArgs);
                break;
            case "LoopCutting":
                new LoopCutting().run(refactoringArgs);
                break;
            case "StripMine":
                // new StripMine();
                break;
            case "TileLoops":
                // new TileLoops();
                break;
            case "Unroll":
                new Unroll().run(refactoringArgs);
                break;
            }
        }
    }

}
