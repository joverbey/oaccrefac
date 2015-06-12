package edu.auburn.oaccrefac.core.newtmp;

public class DirectionVector implements ITuple {

    private final Direction[] elements;
    
    public DirectionVector(Direction... elements) {
        this.elements = elements;
    }
    
    @Override
    public Direction[] getElements() {
        return this.elements;
    }

    @Override
    public Direction elementAt(int whichElement) {
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
