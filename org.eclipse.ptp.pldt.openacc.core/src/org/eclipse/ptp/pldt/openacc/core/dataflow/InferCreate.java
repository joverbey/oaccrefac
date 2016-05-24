package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
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
	
	private boolean anyDefReachingConstructIsOutside(IBinding V, IASTStatement K) {
		for(IASTName D : rd.reachingDefinitions(K)) {
			if(!ASTUtil.isAncestor(K, D) && D.resolveBinding().equals(V)) {
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
			if(!ASTUtil.isAncestor(K, D) && D.resolveBinding().equals(V)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean varIsDeclaredInConstruct(IBinding V, IASTStatement K) {
		for(IASTName name : ASTUtil.find(K, IASTName.class)) {
			if(name.resolveBinding().equals(V)
					&& ASTUtil.findNearestAncestor(name, IASTDeclarationStatement.class) != null 
					&& ASTUtil.isDefinition(name)) {
				return true;
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
