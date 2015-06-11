package edu.auburn.oaccrefac.internal.core;
import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.junit.Before;
import org.junit.Test;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.IndexExpression;
import edu.auburn.oaccrefac.internal.core.IndexParser;
import edu.auburn.oaccrefac.internal.core.Pair;

public class IndexParserTest {
    private IndexParser parser;

    @Before
    public void setUp() {
        parser = new IndexParser();
    }

    public List<Pair<IASTBinaryExpression, IndexExpression>> getExpressionTests(String file) throws CoreException {
        IASTTranslationUnit tu = ASTUtil.translationUnitForFile(file);
        List<Pair<IASTBinaryExpression, IndexExpression>> cases = new LinkedList<Pair<IASTBinaryExpression, IndexExpression>>();

        for (IASTExpressionList exprList : ASTUtil.find(tu, IASTExpressionList.class)) {
            IASTBinaryExpression binExpr = (IASTBinaryExpression) exprList.getExpressions()[0];
            String expected = String.valueOf(((IASTLiteralExpression) exprList.getExpressions()[1]).getValue());
            IndexExpression expectedIndex = new IndexExpression();
            for (String pair : expected.replaceAll("\"", "").split(",")) {
                String[] kv = pair.split(":");
                if (kv.length == 1) {
                    expectedIndex.setConstantFactor(Integer.parseInt(kv[0]));
                } else {
                    expectedIndex.addVariable(kv[0], Integer.parseInt(kv[1]));
                }
            }

            cases.add(new Pair<IASTBinaryExpression, IndexExpression>(binExpr, expectedIndex));
        }

        return cases;
    }

    public void verifyTestResults(IndexExpression index, String expected) {
    }

    @Test
    public void testParsesBasicExpressions() throws CoreException {
        for (Pair<IASTBinaryExpression, IndexExpression> test : getExpressionTests("fixtures/index-parser/t1.cpp")) {
            parser.parseBinaryExpression(test.getFirst());
            assertEquals(test.getSecond(), parser.getIndexExpression());
            parser = new IndexParser();
        }
    }
}
