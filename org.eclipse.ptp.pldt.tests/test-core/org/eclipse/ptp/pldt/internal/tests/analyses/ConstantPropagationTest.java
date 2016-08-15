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
package org.eclipse.ptp.pldt.internal.tests.analyses;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.dataflow.ConstantPropagation;
import org.eclipse.ptp.pldt.openacc.internal.core.dataflow.ExpressionEvaluator;

import junit.framework.TestCase;

public class ConstantPropagationTest extends TestCase {

    public void testBasic() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int one = 1, two, three, n, m;\n" +
                /* 3 */ "  two = 2;\n" +
                /* 4 */ "  three = two;\n" +
                /* 5 */ "  three =\n" +
                /* 6 */ "          three + one;\n" +
                /* 7 */ "  for (int i = 1;\n" +
                /* 8 */ "       i < 3;\n" +
                /* 9 */ "       i++) {\n" +
                /* 10 */ "    n = 4;\n" +
                /* 11 */ "  }\n" +
                /* 12 */ "  m = 5 % 3;\n" +
                /* 13 */ "  m = 3 / two;\n" +
                /* 14 */ "}";
        String[] expectedValues = { //
                "Line 2: one = 1", //
                "Line 3: two = 2", //
                "Line 4: three = 2", //
                "Line 4: two = 2", //
                "Line 5: three = 3", //
                "Line 6: three = 2", //
                "Line 6: one = 1", //
                "Line 7: i = 1", //
                "Line 10: n = 4", //
                "Line 12: m = 2", //
                "Line 13: m = 1", //
                "Line 13: two = 2", //
        };
        check(program, expectedValues);
    }

    public void testIf1() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int n;\n" +
                /* 3 */ "  if (1)\n" +
                /* 4 */ "    n = 1;\n" +
                /* 5 */ "  else\n" +
                /* 6 */ "    n = 1;\n" +
                /* 7 */ "  n;\n" +
                /* 14 */ "}";
        String[] expectedValues = { //
                "Line 4: n = 1", //
                "Line 6: n = 1", //
                "Line 7: n = 1", //
        };
        check(program, expectedValues);
    }

    public void testIf2() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int n;\n" +
                /* 3 */ "  if (1)\n" +
                /* 4 */ "    n = 1;\n" +
                /* 5 */ "  else\n" +
                /* 6 */ "    n = 2;\n" +
                /* 7 */ "  n;\n" +
                /* 14 */ "}";
        String[] expectedValues = { //
                "Line 4: n = 1", //
                "Line 6: n = 2", //
        };
        check(program, expectedValues);
    }

    public void testPointer() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int n = 1, p = &n;\n" +
                /* 3 */ "  n = 2;\n" +
                /* 4 */ "  *p = 3;\n" +
                /* 5 */ "  n;\n" +
                /* 6 */ "}";
        String[] expectedValues = { //
                "Line 2: n = 1", //
                "Line 3: n = 2", //
        };
        check(program, expectedValues);
    }

    public void testGlobal() throws CoreException {
        String program = "int global;\n" +
                /* 2 */ "void foo();\n" +
                /* 3 */ "void main() {\n" +
                /* 4 */ "  global = 1;\n" +
                /* 5 */ "  foo();\n" +
                /* 6 */ "  global;\n" +
                /* 7 */ "}";
        String[] expectedValues = { "Line 4: global = 1\nLine 6: global = 1" };
        check(program, expectedValues);
    }

    public void testFnCall() throws CoreException {
        String program = "void foo();\n" +
                /* 2 */ "void main() {\n" +
                /* 3 */ "  int var = 1;\n" +
                /* 4 */ "  foo();\n" +
                /* 5 */ "  var;\n" +
                /* 6 */ "}";
        String[] expectedValues = { "Line 3: var = 1\nLine 5: var = 1" };
        check(program, expectedValues);
    }

    public void testFnCallAlias() throws CoreException {
        String program = "void foo();\n" +
                /* 2 */ "void main() {\n" +
                /* 3 */ "  int var = 1, ptr = &var;\n" +
                /* 4 */ "  foo(*ptr = 2);\n" +
                /* 5 */ "  var;\n" +
                /* 6 */ "}";
        String[] expectedValues = { "Line 3: var = 1" };
        check(program, expectedValues);
    }

    public void testReturnVal() throws CoreException {
        String program = "int main() {\n" +
                /* 2 */ "  int var = 1;\n" +
                /* 3 */ "  return (var = 2);\n" +
                /* 4 */ "}";
        String[] expectedValues = { "Line 2: var = 1\nLine 3: var = 2" };
        check(program, expectedValues);
    }

    public void testReturn() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int var = 1;\n" +
                /* 3 */ "  return;\n" +
                /* 4 */ "  var;\n" +
                /* 5 */ "}";
        String[] expectedValues = { "Line 2: var = 1" };
        check(program, expectedValues);
    }

    public void testIfElseDifferent() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int var = 1;\n" +
                /* 3 */ "  if (2 < 3) var = 2; else var = 3;\n" +
                /* 4 */ "  var;\n" +
                /* 5 */ "}";
        String[] expectedValues = { "Line 2: var = 1\nLine 3: var = 2\nLine 3: var = 3" };
        check(program, expectedValues);
    }

    public void testIfElseSame() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int var = 1;\n" +
                /* 3 */ "  if (2 < 3) var = 2; else var = 2;\n" +
                /* 4 */ "  var;\n" +
                /* 5 */ "}";
        String[] expectedValues = { "Line 2: var = 1\nLine 3: var = 2\nLine 4: var = 2" };
        check(program, expectedValues);
    }

    public void testIfOnly() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int var = 1;\n" +
                /* 3 */ "  if (2 < 3) var = 2;\n" +
                /* 4 */ "  var;\n" +
                /* 5 */ "}";
        String[] expectedValues = { "Line 2: var = 1\nLine 3: var = 2" };
        check(program, expectedValues);
    }

    public void testFnCallAlias2() throws CoreException {
        String program = "void foo();\n" +
                /* 2 */ "void main() {\n" +
                /* 3 */ "  int var = 1, ptr = &var;\n" +
                /* 4 */ "  foo();\n" +
                /* 5 */ "  var;\n" +
                /* 6 */ "}";
        String[] expectedValues = { "Line 3: var = 1" };
        check(program, expectedValues);
    }

    public void testConditionalPointer() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  int n = 1, *p = &n, m;\n" +
                /* 3 */ "  m = (*p = 2) > 1 ? 3 : 4;\n" +
                /* 4 */ "  n;\n" + // n's constant value cannot be tracked here
        /* 5 */ "}";
        String[] expectedValues = { //
                "Line 2: n = 1", //
                "Line 3: m = 3", //
        };
        check(program, expectedValues);
    }

    public void testLong() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  long n = 5000000000L, m;\n" +
                /* 3 */ "  m = 2*n;\n" +
                /* 4 */ "}";
        String[] expectedValues = { //
                "Line 2: n = 5000000000", //
                "Line 3: m = 10000000000", //
                "Line 3: n = 5000000000", //
        };
        check(program, expectedValues);
    }

    public void testFloat() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  float n = 1.0;\n" +
                /* 3 */ "  n;\n" +
                /* 4 */ "}";
        String[] expectedValues = {};
        check(program, expectedValues);
    }

    public void testChar() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  char n = 32;\n" +
                /* 3 */ "  n;\n" +
                /* 4 */ "}";
        String[] expectedValues = { };
        check(program, expectedValues);
    }

    public void testShortOverflow() throws CoreException {
        String program = "void main() {\n" +
                /* 2 */ "  short n = 32766, m;\n" +
                /* 3 */ "  m = n + 1;\n" +
                /* 4 */ "  m = n + 2;\n" + // Overflow
                /* 5 */ "}";
        String[] expectedValues = { //
                "Line 2: n = 32766", //
                "Line 3: n = 32766", //
                "Line 3: m = 32767", //
                "Line 4: n = 32766", //
        };
        check(program, expectedValues);
    }

    public void testArrayWriteForUnroll() throws CoreException {
        String program = "int main() {\n" + 
                /* 2 */ "    int a[10];\n" + 
                /* 3 */ "    int n = 10;\n" + 
                /* 4 */ "    for (int i = 0; i < n; i++) {\n" + 
                /* 5 */ "        a[i] = 0;\n" + 
                /* 6 */ "    }\n" + 
                /* 7 */ "}";
        String[] expectedValues = { //
                "Line 3: n = 10", //
                "Line 4: i = 0", //
                "Line 4: n = 10", //
        };
        check(program, expectedValues);
    }

    public void testNestedArrayWrite() throws CoreException {
        String program = "int main() {\n" + 
                /* 2 */ "    int a[10][20];\n" + 
                /* 3 */ "    int n = 10, m = 20;\n" + 
                /* 4 */ "    for (int i = 0; i < n; i++) {\n" + 
                /* 5 */ "      for (int j = 0; j < m; j++) {\n" + 
                /* 6 */ "        a[i][j] = 0;\n" + 
                /* 7 */ "      }\n" + 
                /* 8 */ "    }\n" + 
                /* 9 */ "}";
        String[] expectedValues = { //
                "Line 3: n = 10", //
                "Line 3: m = 20", //
                "Line 4: i = 0", //
                "Line 4: n = 10", //
                "Line 5: j = 0", //
                "Line 5: m = 20", //
        };
        check(program, expectedValues);
    }

    public void testGemm() throws CoreException {
        String program = "void gemm(){\n" + 
                /* 02 */ "  int i = 0, j = 0, k = 0, n = 512;\n" + 
                /* 03 */ "  double* C = NULL;\n" + 
                /* 04 */ "  double* H = NULL;\n" + 
                /* 05 */ "  H = (double*)malloc(n*n * sizeof(double));\n" + 
                /* 06 */ "  for (i = 0; i < n; i++){\n" + 
                /* 07 */ "    for (j = 0; j < n; j++){\n" + 
                /* 08 */ "      H[i*n+j] = C[i*n+j];\n" + 
                /* 09 */ "      C[i*n+j] = rand();" + 
                /* 10 */ "    }\n" + 
                /* 11 */ "  }\n" + 
                /* 12 */ "}";
        String[] expectedValues = { //
                "Line 2: i = 0", //
                "Line 2: j = 0", //
                "Line 2: k = 0", //
                "Line 2: n = 512", //
                "Line 6: i = 0", //
                "Line 6: n = 512", //
                "Line 7: j = 0", //
                "Line 7: n = 512", //
        };
        check(program, expectedValues);
    }

    public void testAssignments() throws CoreException {
        String program = "int main() {\n" + 
                "    int n = 1;\n" + 
                "    n += 2;\n" + 
                "    n *= 3;\n" + 
                "    n -= (n-7);\n" + 
                "    return n;\n" + 
                "}";
        String[] expectedValues = { //
                "Line 2: n = 1", //
                "Line 3: n = 3", //
                "Line 4: n = 9", //
                "Line 5: n = 7", //
                "Line 5: n = 9", //
                "Line 6: n = 7", //
        };
        check(program, expectedValues);
    }

    public void testMultAssignInLoop35() throws CoreException {
        String program = "int main() {\n" + 
                "    int i, j, n = 1024;\n" + 
                "    double C[1024], beta = 0.1;\n" + 
                "\n" + 
                "    for (i = 0; i < n; i++) {\n" + 
                "        for (j = 0; j < n; j++) {\n" + 
                "            C[i * n + j] *= beta;\n" + 
                "        }\n" + 
                "    }\n" + 
                "    return 0;\n" + 
                "}";
        String[] expectedValues = { //
                "Line 2: n = 1024", //
                "Line 5: i = 0", //
                "Line 6: j = 0", //
                "Line 5: n = 1024", //
                "Line 6: n = 1024", //
        };
        check(program, expectedValues);
    }

    private void check(String program, String[] expectedValues) throws CoreException {
        IASTTranslationUnit translationUnit = ASTUtil.translationUnitForString(program);
        IASTFunctionDefinition main = ASTUtil.findFirst(translationUnit, IASTFunctionDefinition.class);

        final ConstantPropagation analysis = new ConstantPropagation(main);
        final Set<String> values = new TreeSet<String>();
        main.accept(new ASTVisitor(true) {
            @Override
            public int visit(IASTName name) {
                Long constantValue = analysis.getConstantValue(name);
                if (constantValue != null) {
                    values.add(String.format("Line %d: %s = %s", name.getFileLocation().getStartingLineNumber(), name,
                            constantValue));
                }
                return PROCESS_CONTINUE;
            }
        });
        String actual = toString(values, "\n");

        Arrays.sort(expectedValues);
        String expected = toString(Arrays.asList(expectedValues), "\n");

        assertEquals(expected, actual);
    }

    private String toString(Iterable<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : list) {
            if (!first)
                sb.append(separator);
            first = false;
            sb.append(s);
        }
        return sb.toString();
    }

    public void testOverflowChecks() {
        long[] values = { Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 10, -20, -11, -10, -9, -5, -1, 0, //
                1, 5, 9, 10, 11, 20, Long.MAX_VALUE - 1, Long.MAX_VALUE - 10, Long.MAX_VALUE };
        for (long v1 : values) {
            for (long v2 : values) {
                long sum = v1 + v2;
                long difference = v1 - v2;
                long product = v1 * v2;

                BigInteger b1 = BigInteger.valueOf(v1);
                BigInteger b2 = BigInteger.valueOf(v2);
                BigInteger bsum = b1.add(b2);
                BigInteger bdifference = b1.subtract(b2);
                BigInteger bproduct = b1.multiply(b2);

                assertEquals(!bsum.equals(BigInteger.valueOf(sum)), ExpressionEvaluator.addWillOverflow(v1, v2));
                assertEquals(!bdifference.equals(BigInteger.valueOf(difference)), ExpressionEvaluator.subtractWillOverflow(v1, v2));
                assertEquals(!bproduct.equals(BigInteger.valueOf(product)), ExpressionEvaluator.multiplyWillOverflow(v1, v2));
            }
        }
    }
}
