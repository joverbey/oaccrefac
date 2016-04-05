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
            "acos", "asin", "atan", "atan2", "cos", "cosh", "exp", "fabs", "fmax", "fmin", "log", "log10", "pow", "sin",
            "sinh", "sqrt", "tan", "tanh",
            // Single-precision
            "acosf", "asinf", "atanf", "atan2f", "cosf", "coshf", "expf", "fabsf", "fmaxf", "fminf", "logf", "log10f",
            "powf", "sinf", "sinhf", "sqrtf", "tanf", "tanhf", }));

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
