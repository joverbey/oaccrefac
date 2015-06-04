package edu.auburn.oaccrefac.core.newtmp;

import java.util.Arrays;
import java.util.List;

/**
 * An immutable tuple type for integers
 * 
 * @author Alexander
 *
 */
public class IntegerTuple implements Tuple {

    private final Integer[] elements;

    public IntegerTuple(Integer... elements) {
        this.elements = elements;
    }
    
    public Integer[] getElements() {
        return this.elements;
    }
    
    public Integer elementAt(int whichElement) {
        try {
            return elements[whichElement];
        }
        catch(ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Tuple index out of range");
        }
    }
    
    public int size() {
        return elements.length;
    }
    
    
}
