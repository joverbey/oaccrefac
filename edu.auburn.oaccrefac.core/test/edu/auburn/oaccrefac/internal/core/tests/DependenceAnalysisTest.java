/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package edu.auburn.oaccrefac.internal.core.tests;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.NullProgressMonitor;

import edu.auburn.oaccrefac.core.dependence.DataDependence;
import edu.auburn.oaccrefac.core.dependence.DependenceAnalysis;
import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import junit.framework.TestCase;

public class DependenceAnalysisTest extends TestCase {

    public void testScalarAssignments() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
                /* 2 */ "  int one = 1, two, three;\n" +
                /* 3 */ "  two = 2;\n" +
                /* 4 */ "  three = two;\n" +
                /* 5 */ "  three = three + one;\n" +
                /* 6 */ "}");
        String[] expected = new String[] { //
                "FLOW 2 -> 4 []", //
                "FLOW 2 -> 5 []", //
                "OUTPUT 2 -> 3 []", //
                "OUTPUT 2 -> 4 []", //
                "OUTPUT 2 -> 5 []", //
                "FLOW 3 -> 4 []", //
                "OUTPUT 4 -> 5 []", //
                "FLOW 4 -> 5 []" };
        assertDependencesEqual(expected, stmt);
    }

    public void testScalarAssignmentsInLoop() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
                /* 2 */ "  int v = 1;\n" +
                /* 3 */ "  for (int i = 0; i < 10; i++) {\n" +
                /* 4 */ "    v = v + 1;\n" +
                /* 5 */ "  }\n" +
                /* 6 */ "}");
        String[] expected = new String[] { //
                "FLOW 2 -> 4 []", //
                "OUTPUT 2 -> 4 []", //
                "FLOW 4 -> 4 [*]", //
                "OUTPUT 4 -> 4 [*]", //
                "ANTI 4 -> 4 [*]" };
        assertDependencesEqual(expected, stmt);
    }

    public void testArrayAssignments() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
                /* 2 */ "  int scalar, array[2];\n" +
                /* 3 */ "  scalar = 1;\n" +
                /* 4 */ "  array[0] = scalar;\n" +
                /* 5 */ "  array[1] = array[0];\n" +
                /* 6 */ "  array[0] = array[0];\n" +
                /* 7 */ "}");
        String[] expected = new String[] { //
                "OUTPUT 2 -> 3 []", //
                "FLOW 2 -> 4 []", //
                "FLOW 2 -> 5 []", //
                "FLOW 2 -> 6 []", //
                "OUTPUT 2 -> 4 []", //
                "OUTPUT 2 -> 5 []", //
                "OUTPUT 2 -> 6 []", //
                "FLOW 3 -> 4 []", //
                "FLOW 4 -> 5 []", //
                "FLOW 4 -> 6 []", //
                "OUTPUT 4 -> 6 []", //
                "OUTPUT 2 -> 5 []", //
                "ANTI 5 -> 6 []" };
        assertDependencesEqual(expected, stmt);
    }

    public void testArrayAssignmentsInLoop() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
                /* 2 */ "  int scalar = 1, array[10];\n" +
                /* 3 */ "  for (int i = 0; i < 10; i++) {\n" +
                /* 4 */ "    array[i] = scalar;\n" +
                /* 5 */ "    array[i] = array[i] + 1;\n" +
                /* 6 */ "}");
        String[] expected = new String[] { //
                "FLOW 2 -> 4 []", //
                "FLOW 2 -> 5 []", //
                "OUTPUT 2 -> 4 []", //
                "OUTPUT 2 -> 5 []", //
                "FLOW 4 -> 5 [=]", //
                "OUTPUT 4 -> 4 [=]", //
                "OUTPUT 4 -> 5 [=]", //
                "FLOW 5 -> 5 [=]", //
                "OUTPUT 5 -> 4 [=]", //
                "OUTPUT 5 -> 5 [=]", //
                "ANTI 5 -> 4 [=]", //
                "ANTI 5 -> 5 [=]", //
                };
        assertDependencesEqual(expected, stmt);
    }

    public void test2DArrayAssignments1() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
                /* 2 */ "  int matrix[2][2];\n" +
                /* 3 */ "  matrix[1][0] = 0;\n" +
                /* 4 */ "  matrix[1][0] = matrix[0][1];\n" +
                /* 5 */ "  matrix[0][0] = matrix[1][0];\n" +
                /* 6 */ "}");
        String[] expected = new String[] { //
                "OUTPUT 2 -> 3 []", //
                "OUTPUT 2 -> 4 []", //
                "OUTPUT 2 -> 5 []", //
                "OUTPUT 3 -> 4 []", //
                "FLOW 2 -> 4 []", //
                "FLOW 2 -> 5 []", //
                "FLOW 3 -> 5 []", //
                "FLOW 4 -> 5 []" };
        assertDependencesEqual(expected, stmt);
    }

    public void test2DArrayAssignments2() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
                /* 2 */ "  int i, matrix[2][2];\n" +
                /* 3 */ "  matrix[1][0] = 0;\n" +
                /* 4 */ "  matrix[1][0] = matrix[i][1];\n" +
                /* 5 */ "  matrix[0][0] = matrix[1][i];\n" +
                /* 6 */ "}");
        String[] expected = new String[] { // FIXME no direction vectors for user variables
                "FLOW 2 -> 4 []", //
                "FLOW 2 -> 5 []", //
                "OUTPUT 2 -> 3 []", //
                "FLOW 2 -> 4 []", //
                "OUTPUT 2 -> 4 []", //
                "FLOW 2 -> 5 []", //
                "OUTPUT 2 -> 5 []", //
                "OUTPUT 3 -> 4 []", //
                "FLOW 3 -> 5 []",
                "FLOW 4 -> 5 []"};
        assertDependencesEqual(expected, stmt);
    }

    public void testScalability() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{" + //
                "        int kx, ky, n = 10, nl1, nl2, sig;\n" +
                "        int a11, a12, a13, a21, a22, a23, a31, a32, a33;\n" +
                "        int du1[1000], du2[1000], du3[1000];\n" +
                "        int du1[1000], du2[1000], du3[1000];\n" +
                "        int u1[1000][1000][1000], u2[1000][1000][1000], u3[1000][1000][1000];\n" +
                "        for ( kx=1 ; kx<3 ; kx++ ) /*<<<<< 1294, 9, 1311, 13, Kernel8outer */\n" + 
                "        {\n" + 
                "\n" + 
                "           for ( ky=1 ; ky<n ; ky++ ) /*<<<<< 1297, 12, 1311, 13, Kernel8inner */\n" + 
                "           {\n" + 
                "              du1[ky] = u1[nl1][ky+1][kx] - u1[nl1][ky-1][kx];\n" + 
                "              du2[ky] = u2[nl1][ky+1][kx] - u2[nl1][ky-1][kx];\n" + 
                "              du3[ky] = u3[nl1][ky+1][kx] - u3[nl1][ky-1][kx];\n" + 
                "              u1[nl2][ky][kx]=\n" + 
                "                 u1[nl1][ky][kx]+a11*du1[ky]+a12*du2[ky]+a13*du3[ky] + sig*\n" + 
                "                  (u1[nl1][ky][kx+1]-2.0*u1[nl1][ky][kx]+u1[nl1][ky][kx-1]);\n" + 
                "              u2[nl2][ky][kx]=\n" + 
                "                 u2[nl1][ky][kx]+a21*du1[ky]+a22*du2[ky]+a23*du3[ky] + sig*\n" + 
                "                  (u2[nl1][ky][kx+1]-2.0*u2[nl1][ky][kx]+u2[nl1][ky][kx-1]);\n" + 
                "              u3[nl2][ky][kx]=\n" + 
                "                 u3[nl1][ky][kx]+a31*du1[ky]+a32*du2[ky]+a33*du3[ky] + sig*\n" + 
                "                  (u3[nl1][ky][kx+1]-2.0*u3[nl1][ky][kx]+u3[nl1][ky][kx-1]);\n" + 
                "           }\n" + 
                "        }\n" + 
                "}");
        for (int i = 0; i < 30; i++) {
            assertTrue(analyzeDependences(stmt).size() > 0);
        }
    }

    public void testFabsFunctionCall() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
                /* 2 */ "  double two, negtwo;\n" +
                /* 3 */ "  two = fabs(-2.0);\n" +
                /* 4 */ "  negtwo = -two;\n" +
                /* 5 */ "}");
        String[] expected = new String[] { //
                "FLOW 2 -> 4 []", //
                "FLOW 3 -> 4 []",
                "OUTPUT 2 -> 3 []",
                "OUTPUT 2 -> 4 []" };
        assertDependencesEqual(expected, stmt);
    }

    public void testUnknownFunctionCall1() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
                /* 2 */ "  double value;\n" +
                /* 3 */ "  value = some_function(-2.0);\n" +
                /* 4 */ "}");
        String[] expected = new String[] { //
                "OUTPUT 2 -> 3 []" };
        assertDependencesEqual(expected, stmt);
    }

//    public void testUnknownFunctionCall2() throws Exception {
//        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
//                /* 2 */ "  double value, a = 1, b = 2, c = 3;\n" +
//                /* 3 */ "  double *p = &b;\n" +
//                /* 4 */ "  value = some_function(-2.0);\n" +
//                /* 5 */ "  value = a + b + c;\n" +
//                /* 6 */ "}");
//        String[] expected = new String[] { //
//                "OUTPUT 2 -> 3 []" };
//        assertDependencesEqual(expected, stmt);
//    }

    public void testUnknownFunctionCall2() throws Exception {
        IASTStatement stmt = ASTUtil.parseStatement("{\n" +
              /* 2 */ "  double value, a = 1, b = 2, c = 3;\n" +
              /* 3 */ "  double *p = &b;\n" +
              /* 4 */ "  value = some_function(-2.0);\n" +
              /* 5 */ "  value = a + b + c;\n" +
              /* 6 */ "}");
        try {
            analyzeDependences(stmt);
            fail("Dependence testing should have failed due to function call");
        } catch (DependenceTestFailure x) {
            // Good -- Should not be able to analyze dependences here
        }
    }

    private void assertDependencesEqual(String[] expectedStrings, IASTStatement stmt) throws DependenceTestFailure {
        TreeSet<String> expected = new TreeSet<String>(Arrays.asList(expectedStrings));

        TreeSet<String> actual = new TreeSet<String>();
        for (DataDependence dep : analyzeDependences(stmt))
            actual.add(dep.toString());

        assertEquals(stringify(expected), stringify(actual));
    }

    private Set<DataDependence> analyzeDependences(IASTStatement stmt) throws DependenceTestFailure {
        return new DependenceAnalysis(new NullProgressMonitor(), stmt).getDependences();
    }

    private String stringify(TreeSet<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String elt : set) {
            sb.append(elt);
            sb.append('\n');
        }
        return sb.toString();
    }
}
