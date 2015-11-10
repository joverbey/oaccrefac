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
            System.err.println("FuseLoops");
            System.err.println("IntroduceKernelsLoop");
            System.err.println("IntroduceParallelLoop");
            System.err.println("Unroll");
        } else {
            String[] refactoringArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                refactoringArgs[i-1] = args[i];
            }
            switch (args[0]) {
            case "FuseLoops":
                new FuseLoops().run(refactoringArgs);
                break;
            case "IntroduceKernelsLoop":
                new IntroduceKernelsLoop().run(refactoringArgs);
                break;
            case "IntroduceParallelLoop":
                new IntroduceParallelLoop().run(refactoringArgs);
                break;
            case "Unroll":
                new Unroll().run(refactoringArgs);
                break;
            }
        }
    }

}
