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
package edu.auburn.oaccrefac.core.dependence;

import org.eclipse.cdt.core.dom.ast.IASTStatement;

/**
 * Container for information about a single data dependence
 */
public class DataDependence {

    private final IASTStatement statement1;
    private final IASTStatement statement2;
    private final Direction[] directionVector;
    private final DependenceType type;

    public DataDependence(IASTStatement statement1, IASTStatement statement2, Direction[] directionVector,
            DependenceType type) {
        this.statement1 = statement1;
        this.statement2 = statement2;
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
        return statement1;
    }

    public IASTStatement getStatement2() {
        return statement2;
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
        sb.append(" ");
        sb.append(statement1.getFileLocation().getStartingLineNumber());
        sb.append(" -> ");
        sb.append(statement2.getFileLocation().getStartingLineNumber());
        sb.append(" ");
        sb.append('[');
        for (int i = 0; i < directionVector.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(directionVector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
