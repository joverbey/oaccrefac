package edu.auburn.oaccrefac.core.newtmp;

import org.eclipse.cdt.core.dom.ast.IASTStatement;

/**
 * Class to encapsulate a data dependence 
 * any complex calculations determining dependence information are performed
 * primarily in other classes; this one is simply a container for information
 * 
 * @author Alexander Calvert
 * @author Jeff Overbey
 */
public class DataDependence {

    private final IASTStatement statement1;
    private final IASTStatement statement2;
    private final DirectionVector directionVector;
    private final DependenceType type;

    public DataDependence(IASTStatement statement1, IASTStatement statement2, DirectionVector directionVector,
            DependenceType type) {
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.directionVector = directionVector;
        this.type = type;
    }

    public DirectionVector getDirectionVector() {
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

    public boolean isLoopIndependent() {
        for (Direction direction : this.getDirectionVector().getElements()) {
            if (direction != Direction.EQ) {
                return false;
            }
        }
        return true;
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
        sb.append(directionVector);
        return sb.toString();
    }
}
