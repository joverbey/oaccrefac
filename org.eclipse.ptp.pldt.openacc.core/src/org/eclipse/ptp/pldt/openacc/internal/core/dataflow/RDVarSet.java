/*******************************************************************************
 * Copyright (c) 2015, 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.internal.core.dataflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTPatternUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

/**
 * Reaching definitions entry/exit set for a block. Essentially a mapping of variables to sets of statements
 * that generated a definition that may reach that block's entry/exit. 
 * 
 */
public class RDVarSet {

    private Map<IBinding, Set<IASTNode>> set;

    public RDVarSet() {
        set = new HashMap<IBinding, Set<IASTNode>>();
    }

    /**
     * Removes all of the variable definitions in the given set from the var set. Does not remove the variable from the
     * var set; only changes the set of definitions it is associated with.
     * 
     * @param vars
     */
    public void subtract(RDVarSet definitions) {
        subtract(definitions.set);
    }
    public void subtract(Map<IBinding, Set<IASTNode>> definitions) {
        for (IBinding var : definitions.keySet()) {
            if(set.get(var) == null) {
                set.put(var, new HashSet<IASTNode>());
            }
            set.get(var).removeAll(definitions.get(var));
        }
    }
    
    public void killAll(IBinding var) {
        set.put(var, new HashSet<IASTNode>());
    }

    /**
     * Adds all of the variable definitions in the given set to the var set. Does not overwrite any of the already
     * existing definitions in the set.
     * 
     * @param vars
     */
    public void union(RDVarSet definitions) {
        union(definitions.set);
    }
    public void union(Map<IBinding, Set<IASTNode>> definitions) {
        for (IBinding var : definitions.keySet()) {
            if(!set.containsKey(var)) {
                set.put(var, new HashSet<IASTNode>());
            }
            set.get(var).addAll(definitions.get(var));
        }
    }

    public void add(IBinding var, IASTNode definition) {
    	if(!set.containsKey(var)) {
    		set.put(var, new HashSet<IASTNode>());
    	}
        set.get(var).add(definition);
    }
    
    public Set<IASTNode> get(IBinding var) {
        return set.get(var);
    }
    
    public boolean contains(IASTName definition) {
    	if(!ASTPatternUtil.isDefinition(definition)) {
    		return false;
    	}
        if(!set.containsKey(definition.resolveBinding())) {
        	return false;
        }
        Set<IASTNode> defs = set.get(definition.resolveBinding());
        for(IASTNode defNode : defs) {
        	if(ASTUtil.isAncestor(definition, defNode)) {
        		return true;
        	}
        }
        return false;
    }
    
    public Map<IBinding, Set<IASTNode>> getMap() {
        return Collections.unmodifiableMap(set);
    }
    
    public boolean isEmpty() {
        return set.isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ "); //$NON-NLS-1$
        for(IBinding binding : set.keySet()) {
            for(IASTNode stmt : set.get(binding)) {
                sb.append("(" + binding + ", " + stmt.getFileLocation().getStartingLineNumber() + ") "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        sb.append("]"); //$NON-NLS-1$
        return sb.toString();
    }

}


