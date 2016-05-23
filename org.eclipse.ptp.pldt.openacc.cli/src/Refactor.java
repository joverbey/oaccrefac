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

import java.util.TreeMap;

/**
 * Refactor is a main class for running many different refactorings.
 */
public class Refactor {
    
    private TreeMap<String, Runnable> refactorings;
    
    /**
     * Refactor constructor initializes all refactorings with their runnables.
     */
    public Refactor() {
        refactorings = new TreeMap<>();
        refactorings.put("DistributeLoops", new DistributeLoops());
        refactorings.put("FuseLoops", new FuseLoops());
        refactorings.put("InterchangeLoops", new InterchangeLoops());
        refactorings.put("IntroduceDefaultNone", new IntroduceDefaultNone());
        refactorings.put("IntroduceKernelsLoop", new IntroduceKernelsLoop());
        refactorings.put("IntroduceParallelLoop", new IntroOpenACCLoop());
        refactorings.put("LoopCutting", new LoopCutting());
        refactorings.put("StripMine", new StripMineLoop());
        refactorings.put("TileLoops", new TileLoops());
        refactorings.put("Unroll", new UnrollLoop());
    }
    
    /**
     * main begins refactoring execution
     * 
     * @param args First argument is the refactoring. Second is the file 
     * name. The rest are refactoring specific parameters.
     */
    public static void main(String[] args) { 
        new Refactor().run(args);
    }
    
    /**
     * printUsage prints how the refactoring class is used.
     */
    public void printUsage() {
        System.err.println("Usage: Refactor <refactoring> <args>");
        System.err.println();
        System.err.println("Available refactorings are:");
        for (String key : refactorings.keySet()) {
            System.err.println(key);
        }
    }
    
    /**
     * run begins execution of the actual refactoring.
     * 
     * @param args Arguments to the refactoring.
     */
    public void run(String[] args) {
        if (args.length < 1) {
            printUsage();
        } else {
            String[] refactoringArgs = new String[args.length];
            for (int i = 1; i < args.length; i++) {
                refactoringArgs[i] = args[i];
            }
            Runnable refactoring = refactorings.get(args[0]);
            if (refactoring == null) {
                printUsage();
            } else {
                refactorings.get(args[0]).run(refactoringArgs);
            }
        }
    }

}
