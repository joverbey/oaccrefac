/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert - initial API and implementation
 *******************************************************************************/

package edu.auburn.oaccrefac.core.dataflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;

import edu.auburn.oaccrefac.internal.core.ASTUtil;

/**
 * Reaching definitions entry/exit set for a block. Essentially a mapping of variables to sets of statements
 * that generated a definition that may reach that block's entry/exit. 
 * 
 * @author alexander
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
            if(set.get(var) == null) {
                set.put(var, new HashSet<IASTNode>());
            }
            set.get(var).addAll(definitions.get(var));
        }
    }

    public void add(IBinding var, IASTNode definition) {
        set.get(var).add(definition);
    }
    
    public Set<IASTNode> get(IBinding var) {
        return set.get(var);
    }
    
    public boolean contains(IASTName definition) {
        for(IBinding binding : set.keySet()) {
            if(binding.equals(definition.resolveBinding())) {
                for(IASTNode node : set.get(binding)) {
                    if(ASTUtil.isAncestor(node, definition)) {
                        return true;
                    }
                }
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
        sb.append("[ ");
        for(IBinding binding : set.keySet()) {
            for(IASTNode stmt : set.get(binding)) {
                sb.append("(" + binding + ", " + stmt.getFileLocation().getStartingLineNumber() + ") ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
