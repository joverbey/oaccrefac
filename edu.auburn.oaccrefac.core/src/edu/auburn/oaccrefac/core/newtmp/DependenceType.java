package edu.auburn.oaccrefac.core.newtmp;

public enum DependenceType {

    FLOW, ANTI, OUTPUT, INPUT;

    public static DependenceType forAccesses(VariableAccess v1, VariableAccess v2) {
        if (v1.isWrite() && v2.isRead())
            return FLOW;
        else if (v1.isRead() && v2.isWrite())
            return ANTI;
        else if (v1.isWrite() && v2.isWrite())
            return OUTPUT;
        else
            return INPUT;
    }
    
}
