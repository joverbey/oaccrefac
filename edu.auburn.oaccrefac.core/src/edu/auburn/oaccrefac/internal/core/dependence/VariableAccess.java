package edu.auburn.oaccrefac.internal.core.dependence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.core.dependence.DependenceTestFailure;
import edu.auburn.oaccrefac.core.dependence.DependenceType;

public class VariableAccess {
    private final IASTName variable;
    private final IBinding binding;
    private final LinearExpression[] arraySubscripts;
    private final boolean isWrite;

    public VariableAccess(boolean isWrite, IASTName variable, LinearExpression... arraySubscripts) {
        this.variable = variable;
        this.binding = this.variable.resolveBinding();
        if (arraySubscripts == null || arraySubscripts.length == 0)
            this.arraySubscripts = null;
        else
            this.arraySubscripts = arraySubscripts;
        this.isWrite = isWrite;
    }

    public boolean refersToSameVariableAs(VariableAccess that) throws DependenceTestFailure {
        if (this.binding == null || that.binding == null)
            throw new IllegalStateException("Unable to resolve binding");
        return this.binding.equals(that.binding);
    }

    public IASTName getVariableName() {
        return variable;
    }

    public boolean isScalarAccess() {
        return arraySubscripts == null;
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

    public LinearExpression[] getLinearSubscriptExpressions() {
        return arraySubscripts;
    }

    public int[][] collectCoefficients(List<IBinding> vars) {
        int[][] result = new int[arraySubscripts.length][];
        for (int i = 0; i < arraySubscripts.length; i++) {
            result[i] = collectCoefficients(vars, i);
        }
        return result;
    }

    private int[] collectCoefficients(List<IBinding> vars, int subscript) {
        int[] result = new int[vars.size() + 1];
        result[0] = arraySubscripts[subscript].getConstantCoefficient();
        Map<IBinding, Integer> coeffs = arraySubscripts[subscript].getCoefficients();
        for (int i = 0; i < vars.size(); i++) {
            Integer coeff = coeffs.get(vars.get(i));
            if (coeff == null) {
                result[i + 1] = 0;
            } else {
                result[i + 1] = coeff.intValue();
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(isWrite ? "Write of " : "Read of ");
        sb.append(variable);
        if (arraySubscripts != null) {
            for (int i = 0; i < arraySubscripts.length; i++) {
                sb.append("[");
                sb.append(arraySubscripts[i]);
                sb.append("]");
            }
        }
        return sb.toString();
    }

    public DependenceType getDependenceTypeTo(VariableAccess that) {
        if (this.isWrite() && that.isRead())
            return DependenceType.FLOW;
        else if (this.isRead() && that.isWrite())
            return DependenceType.ANTI;
        else if (this.isWrite() && that.isWrite())
            return DependenceType.OUTPUT;
        else
            return DependenceType.INPUT;
    }
}