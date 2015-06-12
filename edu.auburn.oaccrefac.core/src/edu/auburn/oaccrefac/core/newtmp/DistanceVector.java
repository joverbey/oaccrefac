package edu.auburn.oaccrefac.core.newtmp;

/**
 * represents a distance vector for a data dependency
 * also essentially an immutable tuple type for integers
 * 
 * @author Alexander Calvert
 *
 */
public class DistanceVector implements ITuple {

    private final Integer[] elements;

    public DistanceVector(Integer... elements) {
        this.elements = elements;
    }

    @Override
    public Integer[] getElements() {
        return this.elements;
    }
    
    @Override
    public Integer elementAt(int whichElement) {
        try {
            return elements[whichElement];
        }
        catch(ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Vector index out of range");
        }
    }
    
    @Override
    public int size() {
        return elements.length;
    }
    
    
}
