package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

public class InferCopyout extends InferDataTransfer {

	public InferCopyout(ReachingDefinitions rd, IASTStatement... construct) {
		super(rd, construct);
		infer();
	}
	
	public InferCopyout(IASTStatement... construct) {
		super(construct);
		infer();
	}

	@Override
	protected void infer() {
		
		for(IASTStatement K : topoSorted) {
    		if(tree.isAccAccelRegion(K)) {
    			for(IASTName U : rd.reachedUses(K)) {
    				if(!tree.isAncestor(K, U)) {
    					copies.get(K).add(U.resolveBinding());
    				}
    			}
    		}
    		else {
    			for(IASTStatement C : tree.getChildren(K)) {
    				Set<IBinding> copyoutC = treeSetIBinding();
    				copyoutC.addAll(copies.get(C));
    				for(IBinding V : copyoutC) {
    					if(canPropagateUp(V, K, C, rd)) {
    						copies.get(C).remove(V);
    						copies.get(K).add(V);
    					}
    				}
    			}
    		}
    	}
    	
	}
	
	private boolean canPropagateUp(IBinding V, IASTStatement K, IASTStatement C, ReachingDefinitions rd) {
		for(IASTName U : rd.reachedUses(C)) {
			if(V.equals(U.resolveBinding()) && 
					tree.isAncestor(K, U) && 
					!tree.isAncestor(C, U) && 
					copies.get(C).contains(U.resolveBinding())) { 
				return false;
			}
		}
		return true;
	}
	
}
