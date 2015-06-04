package edu.auburn.oaccrefac.core.newtmp;

import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTStatement;

public class DataDependence {

    //TYPES OF DEPENDENCE
    public static final int FLOW = 0;
    public static final int ANTI = 1;
    public static final int OUTPUT = 2;
    
    //DIRECTION VECTOR VALUES
    public static final int GT = 3;
    public static final int LT = 4;
    public static final int EQ = 5;
    public static final int GE = 6;
    public static final int LE = 7;
    
    private IASTExpressionStatement statement1;
    private IASTExpressionStatement statement2;
    
    private int type;
    private IntegerTuple distanceVector;
    
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
    public IntegerTuple getDirectionVector() {
        Integer[] direction = new Integer[distanceVector.size()];
        for(int i = 0; i < direction.length; i++) {
            if(distanceVector.elementAt(i) > 0) {
                direction[i] = GT;
            }
            else if(distanceVector.elementAt(i) < 0) {
                direction[i] = LT;
            }
            else {
                direction[i] = EQ;
            }
        }
        return new IntegerTuple(direction);
        
    }
    
    public int getType() {
        return type;
    }
    
    public boolean isLoopIndependent() {
        for(int direction : this.getDirectionVector().getElements()) {
            if(direction != EQ) {
                return false;
            }
        }
        return true;
    }
    
}
