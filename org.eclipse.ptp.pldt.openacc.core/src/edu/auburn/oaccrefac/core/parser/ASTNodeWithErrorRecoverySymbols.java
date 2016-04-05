package edu.auburn.oaccrefac.core.parser;

import java.util.Iterator;
import java.util.List;

import edu.auburn.oaccrefac.core.parser.ASTNodeUtil.NonNullIterator;
import edu.auburn.oaccrefac.core.parser.OpenACCParser.ErrorRecoveryInfo;

@SuppressWarnings("all")
public abstract class ASTNodeWithErrorRecoverySymbols extends ASTNode
{
    ErrorRecoveryInfo errorInfo = null;

    @Override public Iterable<? extends IASTNode> getChildren()
    {
        return new Iterable<IASTNode>()
        {
        	@Override public Iterator<IASTNode> iterator()
            {
                return new NonNullIterator<IASTNode>(new Iterator<IASTNode>()
                {
                    private int index = 0;
                    private int numChildren = getNumASTFields();
                    private int numErrorChildren = errorInfo == null ? 0 : errorInfo.getDiscardedSymbols().size();

                    @Override public boolean hasNext()
                    {
                        return index < numChildren + numErrorChildren;
                    }

                    @Override public IASTNode next()
                    {
                        if (index < numChildren)
                            return getASTField(index++);
                        else
                            return errorInfo.<IASTNode>getDiscardedSymbols().get(index++ - numChildren);
                    }

                    @Override public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                });
            }
        };
    }

    public Token getErrorToken()
    {
        return errorInfo == null ? null : errorInfo.errorLookahead;
    }

    public String describeTerminalsExpectedAtErrorPoint()
    {
        return errorInfo == null ? "(none)" : errorInfo.describeExpectedSymbols();
    }

    public List<IASTNode> getSymbolsDiscardedDuringErrorRecovery()
    {
        return errorInfo == null ? null : errorInfo.<IASTNode>getDiscardedSymbols();
    }

    @Override public Object clone()
    {
            ASTNodeWithErrorRecoverySymbols copy = (ASTNodeWithErrorRecoverySymbols)super.clone();
            if (errorInfo != null)
            {
                copy.errorInfo = new ErrorRecoveryInfo(errorInfo.errorState,
                                                       errorInfo.errorLookahead,
                                                       errorInfo.expectedLookaheadSymbols);
                for (IASTNode n : this.getSymbolsDiscardedDuringErrorRecovery())
                {
                    if (n == null)
                        copy.errorInfo.<IASTNode>getDiscardedSymbols().add(null);
                    else
                    {
                        IASTNode newChild = (IASTNode)n.clone();
                        newChild.setParent(copy);
                        copy.errorInfo.<IASTNode>getDiscardedSymbols().add(newChild);
                    }
                }
            }
            return copy;
    }
}
