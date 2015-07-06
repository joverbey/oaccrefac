package edu.auburn.oaccrefac.internal.core.tests;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.junit.Assert;
import org.junit.Test;

import edu.auburn.oaccrefac.internal.core.ASTUtil;
import edu.auburn.oaccrefac.internal.core.ForStatementInquisitor;
import edu.auburn.oaccrefac.internal.core.InquisitorFactory;

//TODO add tests for getLowerBound
//TODO add more code coverage
public class ForStatementInquisitorTest {

    String basic = "for(int i = 0; i < 10; i++);";
    String uncounted = "for(;;);";
    
    private ForStatementInquisitor getInq(String code) {
        IASTStatement stmt;
        try {
            stmt = ASTUtil.parseStatement(code);
        } catch (CoreException e) {
            throw new IllegalArgumentException("bad loop given");
        }
        return InquisitorFactory.getInquisitor((IASTForStatement) stmt);
    }
    
    @Test
    public void test_isCountedLoop()  {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertTrue(inq.isCountedLoop());
    }
    
    @Test
    public void test_isNotCountedLoop() {
        ForStatementInquisitor inq = getInq(uncounted);
        Assert.assertFalse(inq.isCountedLoop());
    }
    
    @Test
    public void test_getIndexVariable() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertEquals(inq.getIndexVariable().getName(), "i");
    }
    
    @Test
    public void test_noIndexVariable() {
        ForStatementInquisitor inq = getInq(uncounted);
        Assert.assertEquals(inq.getIndexVariable(), null);
    }
    
    @Test
    public void test_getInclusiveUpperBound() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertEquals(inq.getInclusiveUpperBound().longValue(), 9);
    }
    
    @Test
    public void test_areAllInnermostStatementsValid() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertTrue(inq.areAllInnermostStatementsValid());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidAsgtBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) i=i-1;");
        Assert.assertTrue(inq.areAllInnermostStatementsValid());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidEmptyCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) {}");
        Assert.assertTrue(inq.areAllInnermostStatementsValid());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidForBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) for(j=0;j<10;j++);");
        Assert.assertTrue(inq.areAllInnermostStatementsValid());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidForCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { for(j=0;j<10;j++) {i=6;} }");
        Assert.assertTrue(inq.areAllInnermostStatementsValid());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidNullCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { ; }");
        Assert.assertTrue(inq.areAllInnermostStatementsValid());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidBadCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { printf(); }");
        Assert.assertFalse(inq.areAllInnermostStatementsValid());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidBadSimpleBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) printf();");
        Assert.assertFalse(inq.areAllInnermostStatementsValid());
    }
    
    @Test
    public void test_getInnermostLoopBody() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertTrue(inq.getInnermostLoopBody() == inq.getStatement().getBody());
    }
    
    @Test
    public void test_getInnermostLoopBodyForStmt() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) for(j=0;j<10;j++);");
        Assert.assertTrue(inq.getInnermostLoopBody() instanceof IASTNullStatement);
    }
    
    @Test
    public void test_getInnermostLoopBodyCompStmt() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) for(j=0;j<10;j++) {i=j;}");
        Assert.assertTrue(inq.getInnermostLoopBody() instanceof IASTCompoundStatement);
        Assert.assertTrue(inq.getInnermostLoopBody().getRawSignature().equals("{i=j;}"));
    }
    
    @Test
    public void test_getInnermostLoopBodyCompStmtWithFor() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { for(j=0;j<10;j++) {i=j;} }");
        Assert.assertTrue(inq.getInnermostLoopBody() instanceof IASTCompoundStatement);
        Assert.assertTrue(inq.getInnermostLoopBody().getRawSignature().equals("{i=j;}"));
    }
    
    @Test
    public void test_getPerfectLoopNestHeaders() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertTrue(inq.getPerfectLoopNestHeaders().get(0) == inq.getStatement());
    }

    @Test
    public void test_getPerfectLoopNestHeadersCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { for(j=0;j<10;j++); }");
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        Assert.assertTrue(headers.size() == 2);
        Assert.assertTrue(headers.get(0).getRawSignature().equals("for(i=0;i<10;i++) { for(j=0;j<10;j++); }"));
        Assert.assertTrue(headers.get(1).getRawSignature().equals("for(j=0;j<10;j++);"));
    }
    
    @Test
    public void test_getPerfectLoopNestHeadersForBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) for(j=0;j<10;j++);");
        List<IASTForStatement> headers = inq.getPerfectLoopNestHeaders();
        Assert.assertTrue(headers.size() == 2);
        Assert.assertTrue(headers.get(0).getRawSignature().equals("for(i=0;i<10;i++) for(j=0;j<10;j++);"));
        Assert.assertTrue(headers.get(1).getRawSignature().equals("for(j=0;j<10;j++);"));
    }
    
    @Test
    public void test_isPerfectLoopNest() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertTrue(inq.isPerfectLoopNest());
    }
    
    @Test
    public void test_isPerfectLoopNestNested() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) for(j=0;j<10;j++);");
        Assert.assertTrue(inq.isPerfectLoopNest());
    }
    
    @Test
    public void test_isPerfectLoopNestCompFor() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { for(j=0;j<10;j++); }");
        Assert.assertTrue(inq.isPerfectLoopNest());
    }
    
    @Test
    public void test_isPerfectLoopNestCompNotFor() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { i=j; }");
        Assert.assertTrue(inq.isPerfectLoopNest());
    }
    
    @Test
    public void test_isPerfectLoopNestComp() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { for(j=0;j<10;j++); }");
        Assert.assertTrue(inq.isPerfectLoopNest());
    }
    
    @Test
    public void test_isNotPerfectLoopNestComp() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { x=1; for(j=0;j<10;j++); }");
        Assert.assertFalse(inq.isPerfectLoopNest());
    }

    @Test
    public void test_getLeadingPragmasNone() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "int x;\n"
                + "for(i=0;i<10;i++);\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findOne(tu, IASTForStatement.class);
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(loop);
        List<IASTPreprocessorPragmaStatement> prags = inq.getLeadingPragmas();
        Assert.assertTrue(prags.size() == 0);
    }
    
    @Test
    public void test_getLeadingPragmasOne() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "#pragma one\n"
                + "for(i=0;i<10;i++);\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findOne(tu, IASTForStatement.class);
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(loop);
        List<IASTPreprocessorPragmaStatement> prags = inq.getLeadingPragmas();
        Assert.assertTrue(prags.size() == 1);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma one"));
    }
    
    @Test
    public void test_getLeadingPragmasTwo() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "#pragma one\n"
                + "#pragma two\n"
                + "for(i=0;i<10;i++);\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findOne(tu, IASTForStatement.class);
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(loop);
        List<IASTPreprocessorPragmaStatement> prags = inq.getLeadingPragmas();
        Assert.assertTrue(prags.size() == 2);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma one"));
        Assert.assertTrue(prags.get(1).getRawSignature().equals("#pragma two"));
    }
   
    @Test
    public void test_getLeadingPragmasSplit() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "#pragma one\n"
                + "int x;\n"
                + "#pragma two\n"
                + "for(i=0;i<10;i++);\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findOne(tu, IASTForStatement.class);
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(loop);
        List<IASTPreprocessorPragmaStatement> prags = inq.getLeadingPragmas();
        Assert.assertTrue(prags.size() == 1);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma two"));
    }
    
    @Test
    public void test_getLeadingPragmasNested() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "#pragma one\n"
                + "for(i=0;i<10;i++) {\n"
                + "  #pragma two\n"
                + "  int x;\n"
                + "  #pragma three\n"
                + "  for(j=0;j<10;j++) {\n"
                + "  }\n"
                + "}\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findOne(tu, IASTForStatement.class);
        ForStatementInquisitor inq = InquisitorFactory.getInquisitor(loop);
        List<IASTPreprocessorPragmaStatement> prags = inq.getLeadingPragmas();
        Assert.assertTrue(prags.size() == 1);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma one"));
        loop = ASTUtil.findOne(loop.getBody(), IASTForStatement.class);
        inq = InquisitorFactory.getInquisitor(loop);
        prags = inq.getLeadingPragmas();
        Assert.assertTrue(prags.size() == 1);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma three"));
        
    }
    
}
