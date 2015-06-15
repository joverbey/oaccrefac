package edu.auburn.oaccrefac.core;

import java.util.Arrays;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTStatement;

import edu.auburn.oaccrefac.core.newtmp.DataDependence;
import edu.auburn.oaccrefac.core.newtmp.DependenceAnalysis;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.fromphotran.DependenceTestFailure;
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
                "OUTPUT 2 -> 4 []", //
                "OUTPUT 2 -> 5 []", //
                "OUTPUT 2 -> 6 []", //
                "FLOW 3 -> 4 []", //
                "FLOW 4 -> 5 []", //
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
                "FLOW 4 -> 5 [*]", //
                "OUTPUT 4 -> 4 [*]", //
                "OUTPUT 4 -> 5 [*]", //
                "FLOW 5 -> 5 [*]", //
                "OUTPUT 5 -> 4 [*]", //
                "OUTPUT 5 -> 5 [*]", //
                "ANTI 5 -> 4 [*]", //
                "ANTI 5 -> 5 [*]", //
                };
        assertDependencesEqual(expected, stmt);
    }

    private void assertDependencesEqual(String[] expectedStrings, IASTStatement stmt) throws DependenceTestFailure {
        TreeSet<String> expected = new TreeSet<String>(Arrays.asList(expectedStrings));

        TreeSet<String> actual = new TreeSet<String>();
        for (DataDependence dep : new DependenceAnalysis().analyze(stmt))
            actual.add(dep.toString());

        assertEquals(stringify(expected), stringify(actual));
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
