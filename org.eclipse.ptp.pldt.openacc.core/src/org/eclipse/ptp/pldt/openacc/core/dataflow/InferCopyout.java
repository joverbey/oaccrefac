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
	
	public InferCopyout(ReachingDefinitions rd, IASTStatement[] construct, IASTStatement... accIgnore) {
		super(rd, construct, accIgnore);
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
    				if(!tree.isAncestor(U, K)) {
    					transfers.get(K).add(U.resolveBinding());
    				}
    			}
    		}
    		else {
    			for(IASTStatement C : tree.getChildren(K)) {
    				Set<IBinding> copyoutC = treeSetIBinding();
    				copyoutC.addAll(transfers.get(C));
    				for(IBinding V : copyoutC) {
    					if(canPropagateUp(V, K, C, rd)) {
    						transfers.get(C).remove(V);
    						transfers.get(K).add(V);
    					}
    				}
    			}
    		}
    	}
    	
	}
	
	private boolean canPropagateUp(IBinding V, IASTStatement K, IASTStatement C, ReachingDefinitions rd) {
		for(IASTName U : rd.reachedUses(C)) {
			if(V.equals(U.resolveBinding()) && 
					tree.isAncestor(U, K) && 
					!tree.isAncestor(U, C) && 
					transfers.get(C).contains(U.resolveBinding())) { 
				return false;
			}
		}
		return true;
	}
	
}
