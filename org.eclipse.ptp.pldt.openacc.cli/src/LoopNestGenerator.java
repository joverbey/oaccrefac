/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Random;

import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

/**
 * Generates random loop nests and assignment statements (for testing refactorings).
 * 
 * @author Jeff Overbey
 */
public class LoopNestGenerator {
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Usage: LoopNestGenerator <count> <seed> <nesting_depth> <num_stmts>"); //$NON-NLS-1$
            System.exit(1);
        }

        int count = Integer.parseInt(args[0]);
        long seed = Long.parseLong(args[1]);
        int depth = Integer.parseInt(args[2]);
        int stmts = Integer.parseInt(args[3]);

        LoopNestGenerator generator = new LoopNestGenerator(seed);
        for (int i = 0; i < count; i++) {
            String filename = String.format("test%04d.c", i); //$NON-NLS-1$
            System.out.printf("Generating %s\n", filename); //$NON-NLS-1$
            Writer out = new BufferedWriter(new FileWriter(filename));
            String fn = generator.generateFunction(depth, stmts);
            out.append(fn);
            out.close();
        }
        System.out.println("Done"); //$NON-NLS-1$
    }

    private final Random rand;

    public LoopNestGenerator() {
        this.rand = new Random();
    }

    public LoopNestGenerator(long seed) {
        this.rand = new Random(seed);
    }

    public String generateFunction(int depth, int stmts) {
        StringBuilder sb = new StringBuilder();
        sb.append("#include <stdio.h>\n"); //$NON-NLS-1$
        sb.append("\n"); //$NON-NLS-1$
        sb.append("static float matrix1[10][10], matrix2[10][10];\n"); //$NON-NLS-1$
        sb.append("\n"); //$NON-NLS-1$
        sb.append("static void init();\n"); //$NON-NLS-1$
        sb.append("static void print();\n"); //$NON-NLS-1$
        sb.append("\n"); //$NON-NLS-1$
        sb.append("int main() {\n"); //$NON-NLS-1$
        sb.append("    init();\n"); //$NON-NLS-1$
        sb.append("    // AUTOTUNE\n"); //$NON-NLS-1$
        sb.append(generateLoopNest(depth, stmts));
        sb.append("    print();\n"); //$NON-NLS-1$
        sb.append("}\n"); //$NON-NLS-1$
        sb.append("\n"); //$NON-NLS-1$
        sb.append("static void init() {\n"); //$NON-NLS-1$
        sb.append("    for (int i = 0; i < 10; i++) {\n"); //$NON-NLS-1$
        sb.append("        for (int j = 0; j < 10; j++) {\n"); //$NON-NLS-1$
        sb.append("            matrix1[i][j] = 10*i + j;\n"); //$NON-NLS-1$
        sb.append("            matrix2[i][j] = -10*j - i;\n"); //$NON-NLS-1$
        sb.append("        }\n"); //$NON-NLS-1$
        sb.append("    }\n"); //$NON-NLS-1$
        sb.append("}\n"); //$NON-NLS-1$
        sb.append("\n"); //$NON-NLS-1$
        sb.append("static void print() {\n"); //$NON-NLS-1$
        sb.append("    printf(\"matrix1 =\");\n"); //$NON-NLS-1$
        sb.append("    for (int i = 0; i < 10; i++) {\n"); //$NON-NLS-1$
        sb.append("        for (int j = 0; j < 10; j++) {\n"); //$NON-NLS-1$
        sb.append("            printf(\" %2.2f\", matrix1[i][j]);\n"); //$NON-NLS-1$
        sb.append("        }\n"); //$NON-NLS-1$
        sb.append("        printf(\"\\n         \");\n"); //$NON-NLS-1$
        sb.append("    }\n"); //$NON-NLS-1$
        sb.append("    printf(\"\\n\");\n"); //$NON-NLS-1$
        sb.append("    printf(\"matrix2 =\");\n"); //$NON-NLS-1$
        sb.append("    for (int i = 0; i < 10; i++) {\n"); //$NON-NLS-1$
        sb.append("        for (int j = 0; j < 10; j++) {\n"); //$NON-NLS-1$
        sb.append("            printf(\" %2.2f\", matrix2[i][j]);\n"); //$NON-NLS-1$
        sb.append("        }\n"); //$NON-NLS-1$
        sb.append("        printf(\"\\n         \");\n"); //$NON-NLS-1$
        sb.append("    }\n"); //$NON-NLS-1$
        sb.append("    printf(\"\\n\");\n"); //$NON-NLS-1$
        sb.append("}\n"); //$NON-NLS-1$
        return ASTUtil.format(sb.toString());
    }

    private String generateLoopNest(int depth, int stmts) {
        depth = Math.min(3, Math.max(0, depth));

        StringBuilder sb = new StringBuilder();
        if (depth >= 1)
            sb.append("for (int i = 1; i < 9; i++) {\n"); //$NON-NLS-1$
        if (depth >= 2)
            sb.append("for (int j = 1; j < 9; j++) {\n"); //$NON-NLS-1$
        if (depth >= 3)
            sb.append("for (int k = 1; k < 9; k++) {\n"); //$NON-NLS-1$
        sb.append(generateBody(depth, stmts));
        if (depth >= 3)
            sb.append("}\n"); //$NON-NLS-1$
        if (depth >= 2)
            sb.append("}\n"); //$NON-NLS-1$
        if (depth >= 1)
            sb.append("}\n"); //$NON-NLS-1$
        return sb.toString();
    }

    private String generateBody(int depth, int assignments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < assignments; i++) {
            sb.append(generateAssignment(depth));
        }
        return sb.toString();
    }

    private String generateAssignment(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateLHS(depth));
        sb.append(" = "); //$NON-NLS-1$
        sb.append(generateRHS(depth));
        sb.append(";\n"); //$NON-NLS-1$
        return sb.toString();
    }

    private String generateLHS(int depth) {
        switch (rand.nextInt(2)) {
        case 0:
            return "matrix1" + generateIndex(depth) + generateIndex(depth); //$NON-NLS-1$
        case 1:
            return "matrix2" + generateIndex(depth) + generateIndex(depth); //$NON-NLS-1$
        default:
            throw new IllegalStateException();
        }
    }

    private String generateIndex(int depth) {
        switch (rand.nextInt(4)) {
        case 0:
            return "[0]"; //$NON-NLS-1$
        case 1:
            return "[" + generateIndexVar(depth) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        case 2:
            return "[" + generateIndexVar(depth) + "+1]"; //$NON-NLS-1$ //$NON-NLS-2$
        case 3:
            return "[" + generateIndexVar(depth) + "-1]"; //$NON-NLS-1$ //$NON-NLS-2$
        default:
            throw new IllegalStateException();
        }
    }

    private String generateIndexVar(int depth) {
        switch (rand.nextInt(depth)) {
        case 0:
            return "i"; //$NON-NLS-1$
        case 1:
            return "j"; //$NON-NLS-1$
        case 2:
            return "k"; //$NON-NLS-1$
        default:
            throw new IllegalStateException();
        }
    }

    private String generateRHS(int depth) {
        switch (rand.nextInt(3)) {
        case 0:
            return "1.0"; //$NON-NLS-1$
        case 1:
            return generateLHS(depth);
        case 2:
            return generateLHS(depth) + " + " + generateRHS(depth); //$NON-NLS-1$
        default:
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unused") // Template
    private String generateX() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }
}
