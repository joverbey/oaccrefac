package edu.auburn.oaccrefac.internal.core.tests;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

import edu.auburn.oaccrefac.core.dataflow.ConstantPropagation;
import edu.auburn.oaccrefac.internal.core.ASTUtil;
import junit.framework.TestCase;

public class ConstPropAnalysisTest extends TestCase {

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
                // Due to a bug in CDT, the for loop does not connect to the statements following it
                // "Line 13: m = 1", //
                // "Line 13: two = 2", //
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
        String[] expectedValues = { "Line 4: global = 1" };
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

    private void check(String program, String[] expectedValues) throws CoreException {
        IASTTranslationUnit translationUnit = ASTUtil.translationUnitForString(program);
        IASTFunctionDefinition main = ASTUtil.findOne(translationUnit, IASTFunctionDefinition.class);

        final ConstantPropagation analysis = new ConstantPropagation(main);
        final Set<String> values = new TreeSet<String>();
        main.accept(new ASTVisitor(true) {
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
}
