package edu.auburn.oaccrefac.core.newtmp;

public class DirectionVector implements ITuple {

    private final Direction[] elements;

    public DirectionVector(Direction... elements) {
        this.elements = elements;
    }

    public DirectionVector(int size) {
        this.elements = new Direction[size];
        for (int i = 0; i < size; i++) {
            this.elements[i] = Direction.ANY;
        }
    }

    @Override
    public Direction[] getElements() {
        return this.elements;
    }

    @Override
    public Direction elementAt(int whichElement) {
        try {
            return elements[whichElement];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Vector index out of range");
        }
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(elements[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
