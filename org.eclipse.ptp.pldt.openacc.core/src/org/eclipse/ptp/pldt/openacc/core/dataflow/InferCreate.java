/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Auburn University - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTPatternUtil;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class InferCreate extends InferDataTransfer {

	public InferCreate(ReachingDefinitions rd, IASTStatement... construct) {
		super(rd, construct);
		infer();
	}
	
	public InferCreate(ReachingDefinitions rd, IASTStatement[] construct, IASTStatement... accIgnore) {
		super(rd, construct, accIgnore);
		infer();
	}
	
	public InferCreate(IASTStatement... construct) {
		super(construct);
		infer();
	}

	@Override
	protected void infer() {
		//TODO: scope check - cannot create var if it is not in scope at the creation point
		for(IASTStatement K : topoSorted) {
    		if(tree.isAccAccelRegion(K)) {
    			for(IBinding V : varsInConstruct(K)) { 
    				//prove that, for V, 
    				//NO defs of V reaching construct are outside
    				//NO uses of V reached by construct are outside
    				if(anyDefReachingConstructIsOutside(V, K)) {
    					continue;
    				}
    				if(anyUseReachedByConstructIsOutside(V, K)) {
    					continue;
    				}
    				if(varIsDeclaredInConstruct(V, K)) {
    					continue;
    				}
    				//if we make it here, no defs outside K reach K and no defs inside K reach outside
    				transfers.get(K).add(V);
    			}
    		}
    		else {
    			//can propagate up if all children have it in their create clauses
    		    //if all children data opt out of data transfer, then there's nothing to make the parent transfer data either
    			//so propagate the intersection of all child create sets
    			Set<IBinding> all = new HashSet<IBinding>();
    			Set<IBinding> kCreate = new HashSet<IBinding>();
    			//get the intersection of all children's create sets
    			//	get all variables in child create sets
    			for(IASTStatement C : tree.getChildren(K)) {
    				all.addAll(transfers.get(C));
    			}
    			
    			/*
    			 * for each V
    			 *     if for every child, V is either in the create set or isn't accessed in the child, 
    			 *         add V to kcreate
    			 */
    			nextV:
    			for(IBinding V : all) {
    				for(IASTStatement C : tree.getChildren(K)) {
    					/*V is not in C's copies and V is accessed in C*/
    					if(!transfers.get(C).contains(V) && varsInConstruct(C).contains(V)) {
    						continue nextV;
    					}
    				}
    				if(K.equals(tree.getRoot())) {
    					if(!varIsDeclaredInConstruct(V, construct)) {
        					kCreate.add(V);
        				}
    				}
    				else {
    					if(!varIsDeclaredInConstruct(V, K)) {
        					kCreate.add(V);
        				}
    				}
    			}
    			
    			//add to this create set, remove from child create sets
    			transfers.put(K, kCreate);
    			for(IASTStatement C : tree.getChildren(K)) {
    				transfers.get(C).removeAll(kCreate);
    			}
    		}
    	}
		
	}
	
	private boolean anyDefReachingConstructIsOutside(IBinding V, IASTStatement K) {
		for(IASTName D : rd.reachingDefinitions(K)) {
			if(!ASTUtil.isAncestor(D, K) && D.resolveBinding().equals(V)) {
				IASTDeclarator decl = ASTUtil.findNearestAncestor(D, IASTDeclarator.class);
				if(decl == null || decl.getInitializer() != null) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean anyUseReachedByConstructIsOutside(IBinding V, IASTStatement K) {
		for(IASTName D : rd.reachedUses(K)) {
			if(!ASTUtil.isAncestor(D, K) && D.resolveBinding().equals(V)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean varIsDeclaredInConstruct(IBinding V, IASTStatement... construct) {
		for(IASTStatement K : construct) {
			for(IASTName name : ASTUtil.find(K, IASTName.class)) {
				if(name.resolveBinding().equals(V)
						&& ASTUtil.findNearestAncestor(name, IASTDeclarationStatement.class) != null 
						&& ASTPatternUtil.isDefinition(name)) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<IBinding> varsInConstruct(IASTStatement statement) {
		Set<IBinding> vars = new HashSet<IBinding>();
		for(IASTName name : ASTUtil.find(statement, IASTName.class)) {
			vars.add(name.resolveBinding());
		}
		return vars;
	}
	
}
