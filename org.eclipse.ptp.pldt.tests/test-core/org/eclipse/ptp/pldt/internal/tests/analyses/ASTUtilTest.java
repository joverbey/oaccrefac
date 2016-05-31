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

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorPragmaStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ASTUtilTest {

    private IASTTranslationUnit translationUnit;

    @Before
    public void setUp() throws CoreException {
        translationUnit = ASTUtil.translationUnitForString( //
                "int main() {\n" + //
                        "    int sum = 0;\n" + //
                        "    int j = sum++;\n" + //
                        "    int k = j + 2*3;\n" + //
                        "}");
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

    @Test
    public void testGetLeadingPragmasNone() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "int x;\n"
                + "for(i=0;i<10;i++);\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findFirst(tu, IASTForStatement.class);
        List<IASTPreprocessorPragmaStatement> prags = ASTUtil.getPragmaNodes(loop);
        Assert.assertTrue(prags.size() == 0);
    }
    
    @Test
    public void testGetLeadingPragmasOne() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "#pragma one\n"
                + "for(i=0;i<10;i++);\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findFirst(tu, IASTForStatement.class);
        List<IASTPreprocessorPragmaStatement> prags = ASTUtil.getPragmaNodes(loop);
        Assert.assertTrue(prags.size() == 1);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma one"));
    }
    
    @Test
    public void testGetLeadingPragmasTwo() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "#pragma one\n"
                + "#pragma two\n"
                + "for(i=0;i<10;i++);\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findFirst(tu, IASTForStatement.class);
        List<IASTPreprocessorPragmaStatement> prags = ASTUtil.getPragmaNodes(loop);
        Assert.assertTrue(prags.size() == 2);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma one"));
        Assert.assertTrue(prags.get(1).getRawSignature().equals("#pragma two"));
    }
   
    @Test
    public void testGetLeadingPragmasSplit() throws CoreException {
        IASTNode tu = ASTUtil.translationUnitForString(""
                + "void main() {\n"
                + "#pragma one\n"
                + "int x;\n"
                + "#pragma two\n"
                + "for(i=0;i<10;i++);\n"
                + "}\n");
        IASTForStatement loop = ASTUtil.findFirst(tu, IASTForStatement.class);
        List<IASTPreprocessorPragmaStatement> prags = ASTUtil.getPragmaNodes(loop);
        Assert.assertTrue(prags.size() == 1);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma two"));
    }
    
    @Test
    public void testGetLeadingPragmasNested() throws CoreException {
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
        IASTForStatement loop = ASTUtil.findFirst(tu, IASTForStatement.class);
        List<IASTPreprocessorPragmaStatement> prags = ASTUtil.getPragmaNodes(loop);
        Assert.assertTrue(prags.size() == 1);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma one"));
        loop = ASTUtil.findFirst(loop.getBody(), IASTForStatement.class);
        prags = ASTUtil.getPragmaNodes(loop);
        Assert.assertTrue(prags.size() == 1);
        Assert.assertTrue(prags.get(0).getRawSignature().equals("#pragma three"));
        
    }
}
