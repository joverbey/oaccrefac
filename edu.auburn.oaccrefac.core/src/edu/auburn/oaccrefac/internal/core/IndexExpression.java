package edu.auburn.oaccrefac.internal.core;
import java.util.HashMap;
import java.util.Map;

public class IndexExpression {
    private int constantFactor;
    private Map<String, Integer> variableFactors;

    public IndexExpression() {
        constantFactor = 0;
        variableFactors = new HashMap<String, Integer>();
    }

    public void addVariable(String name, int factor) {
        variableFactors.put(name, factor);
    }

    public int getConstantFactor() {
        return constantFactor;
    }

    public void setConstantFactor(int constantFactor) {
        this.constantFactor = constantFactor;
    }

    public void addConstantFactor(int constantFactor) {
        this.constantFactor += constantFactor;
    }

    public Map<String, Integer> getVariableFactors() {
        return variableFactors;
    }

    public void setVariableFactors(Map<String, Integer> variableFactors) {
        this.variableFactors = variableFactors;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IndexExpression))
            return false;

        IndexExpression other = (IndexExpression) obj;

        for (String variable : variableFactors.keySet()) {
            if (!other.variableFactors.containsKey(variable)) {
                return false;
            }

            if (other.variableFactors.get(variable) != variableFactors.get(variable)) {
                return false;
            }
        }

        for (String variable : other.variableFactors.keySet()) {
            if (!variableFactors.containsKey(variable)) {
                return false;
            }
        }

        return constantFactor == other.constantFactor;
    }

    public String toString() {
        return String.format("IndexExpression<%d, %s>", constantFactor, variableFactors.toString());
    }
}
