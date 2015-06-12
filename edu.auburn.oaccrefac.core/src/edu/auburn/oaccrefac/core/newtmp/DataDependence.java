package edu.auburn.oaccrefac.core.newtmp;

import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;

/** 
 * Class to encapsulate a data dependence
 * any complex calculations determining dependence information
 * are performed primarily in other classes; this one is simply a 
 * container for information
 * 
 * @author Alexander Calvert
 *
 */
public class DataDependence {
    
    private IASTExpressionStatement statement1;
    private IASTExpressionStatement statement2;
    private DistanceVector distanceVector;
    private DependenceType type;
    
    //need to get info about two iterations of the dependence (ie, distance vector) to set
    //direction vector
    //args: statement1, statement2 - 
    public DataDependence(IASTExpressionStatement statement1, IASTExpressionStatement statement2, DistanceVector distanceVector, DependenceType type) {
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.distanceVector = distanceVector;
        this.type = type;
    }

    /**
     * should identify the direction vector using distance vector information 
     * TODO: might should move this work to the constructor and create an instance variable 
     *  so it only has to get done once
     */
    public DirectionVector getDirectionVector() {
        Direction[] direction = new Direction[distanceVector.size()];
        for(int i = 0; i < direction.length; i++) {
            if(distanceVector.elementAt(i) > 0) {
                direction[i] = Direction.GT;
            }
            else if(distanceVector.elementAt(i) < 0) {
                direction[i] = Direction.LT;
            }
            else {
                direction[i] = Direction.EQ;
            }
        }
        return new DirectionVector(direction);
        
    }
    
    public DependenceType getType() {
        return type;
    }

    public IASTExpressionStatement getStatement1() {
        return statement1;
    }

    public IASTExpressionStatement getStatement2() {
        return statement2;
    }

    public boolean isLoopIndependent() {
        for(Direction direction : this.getDirectionVector().getElements()) {
            if(direction != Direction.EQ) {
                return false;
            }
        }
        return true;
    }
    
}
