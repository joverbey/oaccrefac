/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *     Jeff Overbey (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.dependence;

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.ptp.pldt.openacc.internal.core.dependence.VariableAccess;

/**
 * Container for information about a single data dependence
 * 
 * @author Alexander Calvert
 * @author Jeff Overbey
 */
public class DataDependence {

    private final VariableAccess access1;
    private final VariableAccess access2;
    private final Direction[] directionVector;
    private final DependenceType type;

    public DataDependence(VariableAccess access1, VariableAccess access2, Direction[] directionVector,
            DependenceType type) {
        this.access1 = access1;
        this.access2 = access2;
        this.directionVector = directionVector;
        this.type = type;
    }

    public Direction[] getDirectionVector() {
        return directionVector;
    }

    public DependenceType getType() {
        return type;
    }

    public IASTStatement getStatement1() {
        return access1.getEnclosingStatement();
    }

    public IASTStatement getStatement2() {
        return access2.getEnclosingStatement();
    }
    
    public VariableAccess getAccess1() {
        return access1;
    }

    public VariableAccess getAccess2() {
        return access2;
    }

    public boolean isLoopCarried() {
        return getLevel() > 0;
    }

    public boolean isLoopIndependent() {
        return getLevel() == 0;
    }

    public int getLevel() {
        for (int i = 0; i < directionVector.length; i++) {
            if (directionVector[i] != Direction.EQ) {
                return i + 1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append(" "); //$NON-NLS-1$
        sb.append(getStatement1().getFileLocation().getStartingLineNumber());
        sb.append(" -> "); //$NON-NLS-1$
        sb.append(getStatement2().getFileLocation().getStartingLineNumber());
        sb.append(" "); //$NON-NLS-1$
        sb.append('[');
        for (int i = 0; i < directionVector.length; i++) {
            if (i > 0)
                sb.append(", "); //$NON-NLS-1$
            sb.append(directionVector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Describes this dependence in language intended to be presented to the end user,
     * e.g., in an error message.
     */
    public String toStringForErrorMessage() {
        StringBuilder sb = new StringBuilder();
        String typeString = type.toString().toLowerCase();
        if (isLoopCarried()) {
            sb.append("Loop-carried "); //$NON-NLS-1$
            sb.append(typeString);
        } else {
            sb.append(Character.toUpperCase(typeString.charAt(0)));
            sb.append(typeString.substring(1));
        }
        sb.append(" dependence from line "); //$NON-NLS-1$
        sb.append(getStatement1().getFileLocation().getStartingLineNumber());
        sb.append(" to line "); //$NON-NLS-1$
        sb.append(getStatement2().getFileLocation().getStartingLineNumber());
        sb.append(" "); //$NON-NLS-1$
        sb.append('[');
        for (int i = 0; i < directionVector.length; i++) {
            if (i > 0)
                sb.append(", "); //$NON-NLS-1$
            sb.append(directionVector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
    
}
