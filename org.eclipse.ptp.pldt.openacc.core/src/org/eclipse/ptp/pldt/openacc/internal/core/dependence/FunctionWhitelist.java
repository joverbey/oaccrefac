/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Auburn University - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.dependence;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;

/**
 * List of functions known to be pure (and usable in OpenACC kernels).
 * <p>
 * The list of OpenACC instrinsics supported by the PGI compilers is listed at
 * https://www.pgroup.com/lit/presentations/cea-2.pdf
 * 
 * @author Jeff Overbey
 */
public class FunctionWhitelist {
    private static Set<String> whitelist = new HashSet<String>(Arrays.asList(new String[] {
            // Double-precision
            "acos", "asin", "atan", "atan2", "cos", "cosh", "exp", "fabs", "fmax", "fmin", "log", "log10", "pow", "sin", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$
            "sinh", "sqrt", "tan", "tanh", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            // Single-precision
            "acosf", "asinf", "atanf", "atan2f", "cosf", "coshf", "expf", "fabsf", "fmaxf", "fminf", "logf", "log10f", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$
            "powf", "sinf", "sinhf", "sqrtf", "tanf", "tanhf", })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

    public static boolean isWhitelisted(IASTFunctionCallExpression expr) {
        if (expr.getFunctionNameExpression() instanceof IASTIdExpression) {
            IASTIdExpression id = (IASTIdExpression) expr.getFunctionNameExpression();
            return whitelist.contains(String.valueOf(id.getName().getSimpleID()));
        }
        return false;
    }

    private FunctionWhitelist() {
    }
}
