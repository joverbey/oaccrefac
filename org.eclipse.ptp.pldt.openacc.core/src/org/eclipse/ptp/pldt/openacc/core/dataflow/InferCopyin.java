package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.ptp.pldt.openacc.internal.core.ASTUtil;

public class InferCopyin extends InferDataTransfer {

	public InferCopyin(ReachingDefinitions rd, IASTStatement... construct) {
		super(rd, construct);
		infer();
	}
	
	public InferCopyin(ReachingDefinitions rd, IASTStatement[] construct, IASTStatement... accIgnore) {
		super(rd, construct, accIgnore);
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
    				if(!tree.isAncestor(D, K)) {
    					//special case declaration with no initializer - 
						//assume we want to create (not copy in) if this is the only definition reaching in
    					//see InferCreate
    					IASTDeclarator decl = ASTUtil.findNearestAncestor(D, IASTDeclarator.class);
						if(decl == null || decl.getInitializer() != null) {
							transfers.get(K).add(D.resolveBinding());
						}
    				}
    			}
    		}
    		else {
    			for(IASTStatement C : tree.getChildren(K)) {
    				Set<IBinding> copyinC = treeSetIBinding();
    				copyinC.addAll(transfers.get(C));
    				for(IBinding V : copyinC) {
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
		//if a definition of V is inside K, is not inside C, reaches C, and is being copied into C as it is, we cannot propagate up
		for(IASTName D : rd.reachingDefinitions(C)) {
			if(V.equals(D.resolveBinding()) && 
					tree.isAncestor(D, K) && 
					!tree.isAncestor(D, C) && 
					transfers.get(C).contains(D.resolveBinding())) { 
				return false;
			}
		}
		return true;
	}

}
