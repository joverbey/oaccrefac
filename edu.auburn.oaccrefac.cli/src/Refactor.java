/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John William O'Rourke (Auburn) - initial API and implementation
 *******************************************************************************/

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
            System.err.println("DistributeLoops");
            System.err.println("FuseLoops");
            System.err.println("InterchangeLoops");
            System.err.println("IntroduceDefaultNone:");
            System.err.println("IntroduceKernelsLoop");
            System.err.println("IntroduceParallelLoop");
            System.err.println("LoopCutting");
            System.err.println("StripMine");
            System.err.println("TileLoops");
            System.err.println("Unroll");
        } else {
            // Find way to get rid of this warning
            Main refactoring = getMain(args[0]);
            String[] refactoringArgs = new String[args.length - 1];
            for (int i = 1; i < args.length; i++) {
                refactoringArgs[i-1] = args[i];
            }
            refactoring.run(refactoringArgs);
        }
    }
    
    private Main getMain(String refactoringName) {
        switch (refactoringName) {
        case "DistributeLoops":
            return new DistributeLoops();
        case "FuseLoops":
            return new FuseLoops();
        case "InterchangeLoops":
            return new InterchangeLoops();
        case "IntroduceDefaultNone":
            return new IntroduceDefaultNone();
        case "IntroduceKernelsLoop":
            return new IntroduceKernelsLoop();
        case "IntroduceParallelLoop":
            return new IntroduceParallelLoop();
        case "LoopCutting":
            return new LoopCutting();
        case "StripMine":
            return new StripMine();
        case "TileLoops":
            return new TileLoops();
        case "Unroll":
            return new Unroll();
        default:
            return null;
        }
    }

}
