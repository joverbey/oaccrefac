package edu.auburn.oaccrefac.internal.core.tests;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
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
    String infinite = "for(;;);";
    
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
    public void test_getIndexVariable() {
        ForStatementInquisitor inq = getInq(basic);
        Assert.assertEquals(inq.getIndexVariable().getName(), "i");
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
