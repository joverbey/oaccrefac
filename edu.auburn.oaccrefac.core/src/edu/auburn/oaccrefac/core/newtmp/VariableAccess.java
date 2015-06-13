package edu.auburn.oaccrefac.core.newtmp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.internal.core.Pair;
import edu.auburn.oaccrefac.internal.core.fromphotran.DependenceTestFailure;

public class VariableAccess {
    private final IASTName variable;
    private final IBinding binding;
    private final LinearExpression arraySubscript;
    private final boolean isWrite;

    public VariableAccess(IASTName scalarVariable, boolean isWrite) {
        this.variable = scalarVariable;
        this.binding = this.variable.resolveBinding();
        this.arraySubscript = null;
        this.isWrite = isWrite;
    }

    public VariableAccess(Pair<IASTName, IASTExpression> arrayAccess, boolean isWrite) {
        this.variable = arrayAccess.getFirst();
        this.binding = this.variable.resolveBinding();
        this.arraySubscript = LinearExpression.createFrom(arrayAccess.getSecond());
        this.isWrite = isWrite;
    }

    public boolean refersToSameVariableAs(VariableAccess that) throws DependenceTestFailure {
        if (this.binding == null || that.binding == null)
            throw new IllegalStateException("Unable to resolve binding");
        return this.binding.equals(that.binding);
    }

    public boolean isScalarAccess() {
        return arraySubscript == null;
    }

    public boolean isRead() {
        return !isWrite;
    }

    public boolean isWrite() {
        return isWrite;
    }

    public IASTStatement getEnclosingStatement() {
        IASTNode node = variable.getParent();
        while (node != null && !(node instanceof IASTStatement)) {
            node = node.getParent();
        }
        return node == null ? null : (IASTStatement) node;
    }

    public boolean isInCommonLoopsWith(VariableAccess that) {
        return getCommonEnclosingLoops(that).size() > 0;
    }

    public List<IASTForStatement> getCommonEnclosingLoops(VariableAccess that) {
        List<IASTForStatement> thisLoops = this.getEnclosingLoops();
        List<IASTForStatement> thatLoops = that.getEnclosingLoops();
        List<IASTForStatement> commonLoops = new ArrayList<IASTForStatement>();
        for (int i = 0; i < Math.min(thisLoops.size(), thatLoops.size()); i++) {
            if (thisLoops.get(i).equals(thatLoops.get(i))) {
                commonLoops.add(thisLoops.get(i));
            } else {
                break;
            }
        }
        return commonLoops;
    }

    private List<IASTForStatement> getEnclosingLoops() {
        List<IASTForStatement> enclosingLoops = new LinkedList<IASTForStatement>();
        for (IASTNode node = variable.getParent(); node != null; node = node.getParent()) {
            if (node instanceof IASTForStatement) {
                enclosingLoops.add(0, (IASTForStatement) node);
            }
        }
        return enclosingLoops;
    }

    public int numEnclosingLoops() {
        return getEnclosingLoops().size();
    }

    public boolean enclosingStatementLexicallyPrecedes(VariableAccess that) {
        return this.getEnclosingStatement().getFileLocation().getNodeOffset() < that.getEnclosingStatement()
                .getFileLocation().getNodeOffset();
    }

    public LinearExpression getLinearSubscriptExpression() {
        return arraySubscript;
    }

    public int[] collectCoefficients(IBinding[] vars) {
        int[] result = new int[vars.length + 1];
        result[0] = arraySubscript.getConstantCoefficient();
        Map<IBinding, Integer> coeffs = arraySubscript.getCoefficients();
        for (int i = 0; i < vars.length; i++) {
            Integer coeff = coeffs.get(vars[i]);
            if (coeff == null) {
                result[i+1] = 0;
            } else {
                result[i+1] = coeff.intValue();
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(isWrite ? "Write of " : "Read of ");
        sb.append(variable);
        if (arraySubscript != null) {
            sb.append("[");
            sb.append(arraySubscript);
            sb.append("]");
        }
        return sb.toString();
    }
}