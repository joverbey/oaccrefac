package edu.auburn.oaccrefac.internal.core;
public class Pair<T, V> {
    T first;
    V second;

    public Pair(T first, V second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }

    public String toString() {
        return String.format("Pair<%s, %s>", first.toString(), second.toString());
    }
}
