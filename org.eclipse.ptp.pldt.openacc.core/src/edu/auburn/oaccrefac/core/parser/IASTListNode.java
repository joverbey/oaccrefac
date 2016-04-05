package edu.auburn.oaccrefac.core.parser;

import java.util.List;

@SuppressWarnings("all")
public interface IASTListNode<T> extends List<T>, IASTNode
{
    void insertBefore(T insertBefore, T newElement);
    void insertAfter(T insertAfter, T newElement);
}
