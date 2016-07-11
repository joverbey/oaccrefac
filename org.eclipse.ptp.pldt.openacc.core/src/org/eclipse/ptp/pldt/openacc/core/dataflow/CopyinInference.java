/*******************************************************************************
 * Copyright (c) 2016 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Calvert (Auburn) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

public class CopyinInference extends DataTransferInference {

	public CopyinInference(IASTStatement[] construct, IASTStatement... accIgnore) {
		super(construct, accIgnore);
		infer();
	}
	
	public CopyinInference(IASTStatement... construct) {
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
    					if(!isUninitializedDeclaration(D)) {
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
	
	private boolean canPropagateUp(IBinding V, IASTStatement K, IASTStatement C, ReachingDefinitionsAnalysis rd) {
		//if a definition of V is inside K, is not inside C, reaches C, and is being copied into C as it is, we cannot propagate up
		for(IASTName D : rd.reachingDefinitions(C)) {
			if(V.equals(D.resolveBinding()) && 
					tree.isAncestor(D, K) && 
					!tree.isAncestor(D, C)) { 
				return false;
			}
		}
		return true;
	}

}
