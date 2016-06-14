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
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

public class InferCopy extends InferDataTransfer {
	
	InferCopyin inferCopyin;
	InferCopyout inferCopyout;
	
	public InferCopy(ReachingDefinitions rd, IASTStatement... construct) {
		throw new UnsupportedOperationException("Copy inference should only be done using copyin and copyout inferences");
	}
	
	public InferCopy(IASTStatement... construct) {
		throw new UnsupportedOperationException("Copy inference should only be done using copyin and copyout inferences");
	}
	
	public InferCopy(InferCopyin inferCopyin, InferCopyout inferCopyout) {
		super(null, inferCopyin.construct);
		this.inferCopyin = inferCopyin;
		this.inferCopyout = inferCopyout;
		infer();
	}
	
	public InferCopy(InferCopyin inferCopyin, InferCopyout inferCopyout, IASTStatement... accIgnore) {
		super(null, inferCopyin.construct, accIgnore);
		this.inferCopyin = inferCopyin;
		this.inferCopyout = inferCopyout;
		infer();
	}

	@Override
	protected void infer() {
		Map<IASTStatement, Set<IBinding>> copyin = inferCopyin.get();
		Map<IASTStatement, Set<IBinding>> copyout = inferCopyout.get();
		for(IASTStatement construct : copyin.keySet()) {
    		if(copyout.containsKey(construct)) {
    			Set<IBinding> ins = copyin.get(construct);
    			Set<IBinding> outs = copyout.get(construct);
    			for(IBinding inVar : new HashSet<IBinding>(ins)) {
    				for(IBinding outVar : new HashSet<IBinding>(outs)) {
    					if(inVar.equals(outVar)) {
    						ins.remove(inVar);
    						outs.remove(outVar);
    						//transfers should always contain all constructs in the hierarchy as keys
    						Set<IBinding> gets = transfers.get(construct);
    						gets.add(inVar);
    					}
    				}
    			}
    		}
    	}
	}
	
}