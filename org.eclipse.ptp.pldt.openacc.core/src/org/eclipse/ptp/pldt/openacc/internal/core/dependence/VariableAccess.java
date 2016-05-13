/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.dependence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceTestFailure;
import org.eclipse.ptp.pldt.openacc.core.dependence.DependenceType;

public class VariableAccess implements Comparable<VariableAccess> {
    private final IASTNode variable;
    private final IBinding binding;
    private final LinearExpression[] arraySubscripts;
    private final boolean isWrite;

    public VariableAccess(boolean isWrite, IASTName variable, LinearExpression... arraySubscripts) {
        this.variable = variable;
        this.binding = variable.resolveBinding();
        if (arraySubscripts == null || arraySubscripts.length == 0)
            this.arraySubscripts = null;
        else
            this.arraySubscripts = arraySubscripts;
        this.isWrite = isWrite;
    }

    public VariableAccess(boolean isWrite, IASTNode node, IVariable binding) {
        this.variable = node;
        this.binding = binding;
        this.arraySubscripts = null;
        this.isWrite = isWrite;
    }

    public boolean refersToSameVariableAs(VariableAccess that) throws DependenceTestFailure {
        if (this.binding == null || that.binding == null)
            throw new IllegalStateException("Unable to resolve binding");
        return this.binding.equals(that.binding);
    }

    public IASTNode getVariableName() {
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
    public int compareTo(VariableAccess o) {
        int thisOffset = variable.getFileLocation().getNodeOffset();
        int thatOffset = o.variable.getFileLocation().getNodeOffset();
        return thisOffset - thatOffset;
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

	public boolean bindsTo(IBinding binding) {
        if (this.binding == null)
            throw new IllegalStateException("Unable to resolve binding");
		return this.binding.equals(binding);
	}
}