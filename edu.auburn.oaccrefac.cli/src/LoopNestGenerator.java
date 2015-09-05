
/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
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

import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Generates random loop nests and assignment statements (for testing refactorings).
 * 
 * @author Jeff Overbey
 */
public class LoopNestGenerator {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: LoopNestGenerator <count> <seed>");
            System.exit(1);
        }

        int count = Integer.parseInt(args[0]);
        long seed = Long.parseLong(args[1]);

        LoopNestGenerator generator = new LoopNestGenerator(seed);
        for (int i = 0; i < count; i++) {
            String filename = String.format("test%04d.c", i);
            System.out.printf("Generating %s\n", filename);
            Writer out = new BufferedWriter(new FileWriter(filename));
            String fn = generator.generateFunction();
            out.append(fn);
            out.close();
        }
        System.out.println("Done");
    }

    private final Random rand;

    public LoopNestGenerator() {
        this.rand = new Random();
    }

    public LoopNestGenerator(long seed) {
        this.rand = new Random(seed);
    }

    public String generateFunction() {
        StringBuilder sb = new StringBuilder();
        sb.append("#include <stdio.h>\n");
        sb.append("\n");
        sb.append("static float scalar1 = 1.2, scalar2 = 3.4;\n");
        sb.append("static float array1[10], array2[10];\n");
        sb.append("static float matrix1[10][10], matrix2[10][10];\n");
        sb.append("\n");
        sb.append("static void init() {\n");
        sb.append("    for (int i = 0; i < 10; i++) {\n");
        sb.append("        array1[i] = i;\n");
        sb.append("        array2[i] = 2*i;\n");
        sb.append("        for (int j = 0; j < 10; j++) {\n");
        sb.append("            matrix1[i][j] = 10*i + j;\n");
        sb.append("            matrix2[i][j] = -10*j - i;\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("static void print() {\n");
        sb.append("    printf(\"scalar1 = %2.2f\\n\", scalar1);\n");
        sb.append("    printf(\"scalar2 = %2.2f\\n\", scalar2);\n");
        sb.append("    printf(\"array1 =\");\n");
        sb.append("    for (int i = 0; i < 10; i++) {\n");
        sb.append("        printf(\" %2.2f\", array1[i]);\n");
        sb.append("    }\n");
        sb.append("    printf(\"\\n\");\n");
        sb.append("    printf(\"array2 =\");\n");
        sb.append("    for (int i = 0; i < 10; i++) {\n");
        sb.append("        printf(\" %2.2f\", array2[i]);\n");
        sb.append("    }\n");
        sb.append("    printf(\"\\n\\n\");\n");
        sb.append("    printf(\"matrix1 =\");\n");
        sb.append("    for (int i = 0; i < 10; i++) {\n");
        sb.append("        for (int j = 0; j < 10; j++) {\n");
        sb.append("            printf(\" %2.2f\", matrix1[i][j]);\n");
        sb.append("        }\n");
        sb.append("        printf(\"\\n         \");\n");
        sb.append("    }\n");
        sb.append("    printf(\"\\n\");\n");
        sb.append("    printf(\"matrix2 =\");\n");
        sb.append("    for (int i = 0; i < 10; i++) {\n");
        sb.append("        for (int j = 0; j < 10; j++) {\n");
        sb.append("            printf(\" %2.2f\", matrix2[i][j]);\n");
        sb.append("        }\n");
        sb.append("        printf(\"\\n         \");\n");
        sb.append("    }\n");
        sb.append("    printf(\"\\n\");\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("int main() {\n");
        sb.append("    init();\n");
        sb.append("    // AUTOTUNE\n");
        sb.append(generateLoopNest());
        sb.append("    print();\n");
        sb.append("}\n");
        return ASTUtil.format(sb.toString());
    }

    private String generateLoopNest() {
        int depth = rand.nextInt(3) + 1;

        StringBuilder sb = new StringBuilder();
        if (depth >= 1)
            sb.append("for (int i = 1; i < 9; i++) {\n");
        if (depth >= 2)
            sb.append("for (int j = 1; j < 9; j++) {\n");
        if (depth >= 3)
            sb.append("for (int k = 1; k < 9; k++) {\n");
        sb.append(generateBody(depth));
        if (depth >= 3)
            sb.append("}\n");
        if (depth >= 2)
            sb.append("}\n");
        if (depth >= 1)
            sb.append("}\n");
        return sb.toString();
    }

    private String generateBody(int depth) {
        int assignments = rand.nextInt(5);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < assignments; i++) {
            sb.append(generateAssignment(depth));
        }
        return sb.toString();
    }

    private String generateAssignment(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateLHS(depth));
        sb.append(" = ");
        sb.append(generateRHS(depth));
        sb.append(";\n");
        return sb.toString();
    }

    private String generateLHS(int depth) {
        switch (rand.nextInt(6)) {
        case 0:
            return "scalar1";
        case 1:
            return "scalar2";
        case 2:
            return "array1" + generateIndex(depth);
        case 3:
            return "array2" + generateIndex(depth);
        case 4:
            return "matrix1" + generateIndex(depth) + generateIndex(depth);
        case 5:
            return "matrix2" + generateIndex(depth) + generateIndex(depth);
        default:
            throw new IllegalStateException();
        }
    }

    private String generateIndex(int depth) {
        switch (rand.nextInt(6)) {
        case 0:
            return "[0]";
        case 1:
            return "[5]";
        case 2:
            return "[9]";
        case 3:
            return "[" + generateIndexVar(depth) + "]";
        case 4:
            return "[" + generateIndexVar(depth) + "+1]";
        case 5:
            return "[" + generateIndexVar(depth) + "-1]";
        default:
            throw new IllegalStateException();
        }
    }

    private String generateIndexVar(int depth) {
        switch (rand.nextInt(depth)) {
        case 0:
            return "i";
        case 1:
            return "j";
        case 2:
            return "k";
        default:
            throw new IllegalStateException();
        }
    }

    private String generateRHS(int depth) {
        switch (rand.nextInt(3)) {
        case 0:
            return "1.0";
        case 1:
            return generateLHS(depth);
        case 2:
            return generateLHS(depth) + " + " + generateRHS(depth);
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
