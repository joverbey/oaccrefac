package edu.auburn.oaccrefac.core.newtmp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.layout.GridLayout;

import edu.auburn.oaccrefac.internal.core.FourierMotzkinEliminator;
import edu.auburn.oaccrefac.internal.core.Matrix;
import edu.auburn.oaccrefac.internal.core.fromphotran.Direction;
import edu.auburn.oaccrefac.internal.core.fromphotran.IDependenceTester;

public class FourierMotzkinDependenceTest implements IDependenceTester {

    
    
    /* 
     * assumes that the subscript expressions are normalized
     * see example on Wolfe pg 225-226
     *  
     */
    @Override
    public boolean test(int nestingLevel, int[] lowerBounds, int[] upperBounds, int[][] writeCoefficients,
            int[][] readCoefficients, Direction[] direction) {
        
        FourierMotzkinEliminator el = new FourierMotzkinEliminator();
        Matrix m = new Matrix();
        
        //int numColumns = writeCoefficients[0].length + readCoefficients.length + 1;
        
        //get the inequalities from the subscripts
        for(int i = 0; i < writeCoefficients.length; i++) {
            //row will contain write coeffs, the negative read coeffs, and the constant
            ArrayList<Double> row = new ArrayList<Double>(); 
            for(int j = 1; j < writeCoefficients[i].length; j++) {
                row.add((double) writeCoefficients[i][j]);
            }
            for(int j = 1; j < readCoefficients[i].length; j++) {
                row.add((double) -readCoefficients[i][j]);
            }
            row.add((double) (readCoefficients[i][0] - writeCoefficients[i][0]));
            m.addRowAtIndex(m.getNumRows(), listToPrimitiveArray(row));
            m.addRowAtIndex(m.getNumRows(), negate(listToPrimitiveArray(row)));
        }
        
        //get the inequalities from the loop bounds
//        -i_d1 <= -l1;     -1  0  0 ... -l1
//         i_d1 <=  u1;      1  0  0 ...  u1 
//        -i_u1 <= -l1;      0 -1  0 ... -l1
//         i_u1  <= u1;      0  1  0 ...  u1
//         ...        
        for(int i = 0; i < lowerBounds.length; i++) {
            double[] row1 = new double[writeCoefficients[0].length + readCoefficients.length + 1];
            double[] row2 = new double[writeCoefficients[0].length + readCoefficients.length + 1];
            double[] row3 = new double[writeCoefficients[0].length + readCoefficients.length + 1];
            double[] row4 = new double[writeCoefficients[0].length + readCoefficients.length + 1];
            row1[2*i] = -1;
            row1[row1.length-1] = -lowerBounds[i];
            row2[2*i] = 1;
            row2[row2.length-1] = upperBounds[i];
            row3[2*i+1] = -1;
            row3[row3.length-1] = -lowerBounds[i];
            row4[2*i+1] = -1;
            row4[row1.length-1] = -lowerBounds[i];
            m.addRowAtIndex(m.getNumRows(), row1);
            m.addRowAtIndex(m.getNumRows(), row2);
            m.addRowAtIndex(m.getNumRows(), row3);
            m.addRowAtIndex(m.getNumRows(), row4);
        }        
        
        //get the inequalities from the dependence direction vector
        //iterate through the direction vector, adding an inequality 
        //comparing i_d and i_u for that particular vector element
        for(int i = 0; i < direction.length; i++) {
            switch(direction[i]) {
            case ANY:
                break;
            case EQUALS:
                break;
            case GREATER_THAN:
                break;
            case LESS_THAN:
                
                break;
            }
        }
        
        el.eliminateForIntegerSolutions(m);
        
        return false;
    }
    
    //why this method isnt in the java library already, i dont know. 
    private double[] listToPrimitiveArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for(int i = 0 ; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
    
    //negate all values in an array
    //useful for putting equals relations in a <= matrix
    //ie, (x == 1) <=> (x <= 1) && (-x <= -1)
    private double[] negate(double[] arr) {
        double[] ret = new double[arr.length];
        for(int i = 0; i < arr.length; i++) {
            ret[i] = -arr[i];
        }
        return ret;
    }

}
