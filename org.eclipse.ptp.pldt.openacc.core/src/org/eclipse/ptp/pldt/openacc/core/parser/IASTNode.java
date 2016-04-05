package org.eclipse.ptp.pldt.openacc.core.parser;

import java.io.PrintStream;
import java.util.Set;

@SuppressWarnings("all")
public interface IASTNode extends Cloneable
{
    Object clone();
    IASTNode getParent();
    void setParent(IASTNode parent);
    Iterable<? extends IASTNode> getChildren();
    void accept(IASTVisitor visitor);
    void replaceChild(IASTNode node, IASTNode withNode);
    void removeFromTree();
    void replaceWith(IASTNode newNode);
    void replaceWith(String literalString);
    <T extends IASTNode> Set<T> findAll(Class<T> targetClass);
    <T extends IASTNode> T findNearestAncestor(Class<T> targetClass);
    <T extends IASTNode> T findFirst(Class<T> targetClass);
    <T extends IASTNode> T findLast(Class<T> targetClass);
    Token findFirstToken();
    Token findLastToken();
    boolean isFirstChildInList();
    void printOn(PrintStream out);
}
