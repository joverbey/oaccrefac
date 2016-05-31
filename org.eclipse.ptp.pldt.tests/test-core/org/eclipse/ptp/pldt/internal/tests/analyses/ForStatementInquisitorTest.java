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
package org.eclipse.ptp.pldt.internal.tests.analyses;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ForStatementInquisitor;
import org.junit.Assert;
import org.junit.Test;

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
        return ForStatementInquisitor.getInquisitor((IASTForStatement) stmt);
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
        Assert.assertNull(inq.getFirstUnsupportedStmt());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidAsgtBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) i=i-1;");
        Assert.assertNull(inq.getFirstUnsupportedStmt());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidEmptyCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) {}");
        Assert.assertNull(inq.getFirstUnsupportedStmt());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidForBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) for(j=0;j<10;j++);");
        Assert.assertNull(inq.getFirstUnsupportedStmt());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidForCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { for(j=0;j<10;j++) {i=6;} }");
        Assert.assertNull(inq.getFirstUnsupportedStmt());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidNullCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { ; }");
        Assert.assertNull(inq.getFirstUnsupportedStmt());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidBadCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { printf(); }");
        Assert.assertNotNull(inq.getFirstUnsupportedStmt());
    }
    
    @Test
    public void test_areAllInnermostStatementsValidBadSimpleBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) printf();");
        Assert.assertNotNull(inq.getFirstUnsupportedStmt());
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
        Assert.assertTrue(inq.getPerfectlyNestedLoops().get(0) == inq.getStatement());
    }

    @Test
    public void test_getPerfectLoopNestHeadersCompBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) { for(j=0;j<10;j++); }");
        List<IASTForStatement> headers = inq.getPerfectlyNestedLoops();
        Assert.assertTrue(headers.size() == 2);
        Assert.assertTrue(headers.get(0).getRawSignature().equals("for(i=0;i<10;i++) { for(j=0;j<10;j++); }"));
        Assert.assertTrue(headers.get(1).getRawSignature().equals("for(j=0;j<10;j++);"));
    }
    
    @Test
    public void test_getPerfectLoopNestHeadersForBody() {
        ForStatementInquisitor inq = getInq("for(i=0;i<10;i++) for(j=0;j<10;j++);");
        List<IASTForStatement> headers = inq.getPerfectlyNestedLoops();
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
}
