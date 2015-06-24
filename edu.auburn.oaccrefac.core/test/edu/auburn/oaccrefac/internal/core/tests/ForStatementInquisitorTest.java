package edu.auburn.oaccrefac.internal.core.tests;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
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
        Assert.assertEquals(inq.getInclusiveUpperBound(), 9);
    }
    
    @Test
    public void test_areAllInnermostStatementsValid() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertTrue(inq.areAllInnermostStatementsValid());
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
    public void test_isPerfectLoopNest() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertTrue(inq.isPerfectLoopNest());
    }
    

}
