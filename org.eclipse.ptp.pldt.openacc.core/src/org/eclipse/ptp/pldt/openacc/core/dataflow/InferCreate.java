package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class InferCreate extends InferDataTransfer {

	public InferCreate(ReachingDefinitions rd, IASTStatement... construct) {
		super(rd, construct);
		infer();
	}
	
	public InferCreate(IASTStatement... construct) {
		super(construct);
		infer();
	}

	@Override
	protected void infer() {
		
		for(IASTStatement K : topoSorted) {
    		if(tree.isAccAccelRegion(K)) {
    			nextV:
    			for(IBinding V : varsInConstruct(K)) { 
    				//prove that, for V, 
    				//NO defs reaching construct are outside
    				//NO uses reached construct are outside
    				for(IASTName D : rd.reachingDefinitions(K)) {
    					if(!tree.isAncestor(K, D)) {
    						//special case declaration with no initializer - 
    						//assume we want to create if this is the only definition reaching in
    						IASTDeclarator decl = ASTUtil.findNearestAncestor(D, IASTDeclarator.class);
    						if(decl == null || decl.getInitializer() != null) {
    							continue nextV;
    						}
    					}
    				}
    				for(IASTName D : rd.reachedUses(K)) {
    					if(!tree.isAncestor(K, D)) {
    						continue nextV;
    					}
    				} 
    				//if we make it here, no defs outside K reach K and no defs inside K reach outside
    				copies.get(K).add(V);
    			}
    		}
    		else {
    			//can propagate up if all children have it in their create clauses
    		    //if all children that have the opportunity to transfer data opt out, then there's nothing to make the parent transfer data either
    			//so propagate the intersection of all child create sets
    			Set<IBinding> kCreate = new HashSet<IBinding>();
    			//get the intersection of all children's create sets
    			//	get all variables in child create sets
    			for(IASTStatement C : tree.getChildren(K)) {
    				kCreate.addAll(copies.get(C));
    			}
    			//	remove every variable not in any one of the child create sets 
    			for(IASTStatement C : tree.getChildren(K)) {
    				kCreate.retainAll(copies.get(C));
    			}
    			//add to this create set, remove from child create sets
    			copies.put(K, kCreate);
    			for(IASTStatement C : tree.getChildren(K)) {
    				copies.get(C).removeAll(kCreate);
    			}
    		}
    	}
		
	}

	private Set<IBinding> varsInConstruct(IASTStatement statement) {
		Set<IBinding> vars = new HashSet<IBinding>();
		for(IASTName name : ASTUtil.find(statement, IASTName.class)) {
			vars.add(name.resolveBinding());
		}
		return vars;
	}

}
