package edu.auburn.oaccrefac.core.newtmp;

import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

public class DataDependence {

    //TYPES OF DEPENDENCE
    public static final int FLOW = 0;
    public static final int ANTI = 1;
    public static final int OUTPUT = 2;
    
    //DIRECTION VECTOR VALUES
//    public static final int GT = 3;
//    public static final int LT = 4;
//    public static final int EQ = 5;
//    public static final int GE = 6;
//    public static final int LE = 7;
    
    private IASTExpressionStatement statement1;
    private IASTExpressionStatement statement2;
    
    private int type;
    private DistanceVector distanceVector;
    
    //need to get info about two iterations of the dependence (ie, distance vector) to set
    //direction vector
    //args: statement1, statement2 - 
    public DataDependence(IASTExpressionStatement statement1, IASTExpressionStatement statement2, int type) {
        this.statement1 = statement1;
        this.statement2 = statement2;
        this.type = type;
    }

    /**
     * should identify the direction vector using distance vector information 
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
    
    public int getType() {
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
