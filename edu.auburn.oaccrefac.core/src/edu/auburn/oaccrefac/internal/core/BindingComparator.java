package edu.auburn.oaccrefac.internal.core;

import java.util.Comparator;

import org.eclipse.cdt.core.dom.ast.IBinding;

public class BindingComparator implements Comparator<IBinding> {
    @Override
    public int compare(IBinding b1, IBinding b2) {
        int result = b1.getName().compareTo(b2.getName());
        if (result != 0)
            return result;
        else // Same name, possibly in different scopes
            return System.identityHashCode(b2) - System.identityHashCode(b1);
    }
}