package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

public class InferCopyin extends InferDataTransfer {

	public InferCopyin(ReachingDefinitions rd, IASTStatement... construct) {
		super(rd, construct);
		infer();
	}
	
	public InferCopyin(IASTStatement... construct) {
		super(construct);
		infer();
	}
	
	@Override
	protected void infer() {

		for(IASTStatement K : topoSorted) {
    		if(tree.isAccAccelRegion(K)) {
    			for(IASTName D : rd.reachingDefinitions(K)) {
    				if(!tree.isAncestor(K, D)) {
    					copies.get(K).add(D.resolveBinding());
    				}
    			}
    		}
    		else {
    			for(IASTStatement C : tree.getChildren(K)) {
    				Set<IBinding> copyinC = treeSetIBinding();
    				copyinC.addAll(copies.get(C));
    				for(IBinding V : copyinC) {
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
		//if a definition of V is inside K, is not inside C, reaches C, and is being copied into C as it is, we cannot propagate up
		for(IASTName D : rd.reachingDefinitions(C)) {
			if(V.equals(D.resolveBinding()) && 
					tree.isAncestor(K, D) && 
					!tree.isAncestor(C, D) && 
					copies.get(C).contains(D.resolveBinding())) { 
				return false;
			}
		}
		return true;
	}

}