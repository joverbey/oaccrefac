package edu.auburn.oaccrefac.internal.core;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.junit.Test;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.patternmatching.ASTMatcher;
import edu.auburn.oaccrefac.internal.core.patternmatching.ArbitraryStatement;

/**
 * Unit tests for {@link ASTMatcher}.
 * 
 * @author Jeff Overbey
 */
public class ASTMatcherTest {

    @Test
    public void testSimple() throws CoreException {
        checkMatchExpr("{}", "3", "3");
        checkMatchExpr(null, "3", "5");
        checkMatchExpr("{i=i}", "i", "i");
        checkMatchExpr("{i=j}", "i", "j");
        checkMatchExpr("{}", "3 + 5", "3+ 5");
        checkMatchExpr(null, "3 + 5", "3 + 05");
        checkMatchExpr("{i=foo}", "3*i + 7", "3 * foo + 7");
        checkMatchExpr("{i=eye, j=jay}", "-1*2*(3+(4/i)+(5-6-9%j%j))", "-1*2*(3+(4/eye)+(5-6-9%jay%jay))");
        checkMatchExpr("{i=j}", "i+i", "j+j");
        checkMatchExpr(null, "i+i", "i+j");
        checkMatchExpr(null, "i+i", "j+i");
    }

    @Test
    public void testFor() throws CoreException {
        IASTForStatement forLoop = (IASTForStatement) ASTUtil.parseStatement("for (i = 0; i < n; i++) ;");

        IASTForStatement pattern = forLoop.copy(CopyStyle.withoutLocations);
        pattern.setBody(new ArbitraryStatement());

        checkMatchStmt("{i=i, n=k}", pattern, "for (i = 0; i < k; i++) ;");
        checkMatchStmt("{i=j, n=k}", pattern, "for (j = 0; j < k; j++) ;");
        checkMatchStmt(null, pattern, "for (i = 0; j < n; i++) ;");
        checkMatchStmt(null, pattern, "for (i = 1; i < n; i++) ;");
        checkMatchStmt("{i=ii, n=nn}", pattern, "for (ii = 0; ii < nn; ii++) { f(); g(); break; }");
    }

    private void checkMatchExpr(String expected, String patternString, String exprString) throws CoreException {
        IASTExpression pattern = ASTUtil.parseExpression(patternString);
        IASTExpression expr = ASTUtil.parseExpression(exprString);
        assertMatchEquals(expected, ASTMatcher.unify(pattern, expr));
    }

    private void checkMatchStmt(String expected, IASTStatement pattern, String stmtString) throws CoreException {
        IASTStatement stmt = ASTUtil.parseStatement(stmtString);
        assertMatchEquals(expected, ASTMatcher.unify(pattern, stmt));
    }

    private void assertMatchEquals(String expected, Map<String, String> match) {
        if (match == null)
            Assert.assertEquals(expected, null);
        else
            Assert.assertEquals(expected, match.toString());
    }
}
