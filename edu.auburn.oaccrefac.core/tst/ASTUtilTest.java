import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

public class ASTUtilTest {

    private IASTTranslationUnit translationUnit;

    @Before
    public void setUp() throws CoreException {
        translationUnit = ASTUtil.translationUnitForFile("fixtures/find-in-ast/01-example.cpp");
    }

    @Test
    public void testFindIdExpression() {
        List<IASTIdExpression> results = ASTUtil.find(translationUnit, IASTIdExpression.class);
        // IdExpression is only when a variable is referenced, not when it is
        // declared. In 01-example, sum and j are each referenced once.
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testFindBinaryExpression() {
        List<IASTBinaryExpression> results = ASTUtil.find(translationUnit, IASTBinaryExpression.class);
        // Addition and multiplication on line 4
        Assert.assertEquals(2, results.size());
    }

    @Test
    public void testFindUnaryExpression() {
        List<IASTUnaryExpression> results = ASTUtil.find(translationUnit, IASTUnaryExpression.class);
        // sum++ on line 3
        Assert.assertEquals(1, results.size());
    }
}
