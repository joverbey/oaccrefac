/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.internal.tests.analyses;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceTestFailure;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.LinearExpression;

import junit.framework.TestCase;

public class LinearExpressionMatcherTest extends TestCase {

    public void test() throws CoreException, DependenceTestFailure {
        check("0", "0");
        check("0", "-0");
        check("-1", "-(1+0)");
        check("-6", "-(2*3)");
        check("3", "1 + 2");
        check("2", "(1 + 1)");
        check("7", "2+3+(1 + 1)");
        check("6", "1+2+3");
        check("6", "1+(2+((3)))");
        check("3 + 1*a", "a+3");
        check("5 + 1*a", "a+3+2");
        check("5 + 1*a", "2+a+3");
        check("5 + 1*a", "2+3+a");
        check("5 + 1*a", "2+3+(a)");
        check("0 + 4*a", "4*a");
        check("0 + 4*a", "4*(a)");
        check("0 + 4*a", "4*((a))");
        check("0 + 4*a", "(4)*((a))");
        check("5 + 4*a", "2+3+4*(a)");
        check("5 + 4*a", "2+4*(a)+3");
        check("5 + 4*a", "4*(a)+3+2");
        check("5 + 4*a", "4*(a)+(3+2)");
        check("7 + 4*a", "(1+1)+4*(a)+(3+2)");
        check("5 + 2*a", "2+3+(1+1)*(a)");
        check("5 + 2*a + 4*b", "5 + 2*a + 4*b");
        check("5 + 2*a + 4*b", "5 + 1*b + 2*a + 3*b");
        check("5 + 2*a + -2*b", "5 + 1*b + 2*a - 3*b");
        check("5 + 2*a + -2*b", "5 + 1*b + 2*a - 2*b + 1*(-b)");
        check("1 + 1*a + 1*b", "1 + (a + b)");
        //check("1 + 2*a + 2*b", "1 + 2*(a + b)");
        check("0 + 2*a + 1*b + 1*c", "2*a + (b + c))");
        check("0 + 6*a", "2*a*3)");
        check("0 + -2*a + -3*b", "-(2*a+3*b)");
        check("0 + -1*a + -1*b + 1*c + 1*d + 1*e", "-(a+b)+(c+d+e)");
        check("0 + -1*a + -1*b + 1*c + 1*d + 1*e", "-(a+b+-c)+(d+e)");
    }

    private void check(String expected, String expression) throws DependenceTestFailure, CoreException {
        IASTCompoundStatement stmt = (IASTCompoundStatement) ASTUtil.parseStatement("{ int a, b, c, d, e, f, g; " + expression + "; }");
        IASTExpression expr = ((IASTExpressionStatement)stmt.getStatements()[1]).getExpression();
        LinearExpression le = LinearExpression.createFrom(expr);
        assertEquals(expected, le.toString());
    }
}
