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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

public class CopyInference extends DataTransferInference {
	
	private final CopyinInference inferCopyin;
	private final CopyoutInference inferCopyout;
	
	public CopyInference(ReachingDefinitionsAnalysis rd, IASTStatement... construct) {
		throw new UnsupportedOperationException("Copy inference should only be done using copyin and copyout inferences"); //$NON-NLS-1$
	}
	
	public CopyInference(IASTStatement... construct) {
		throw new UnsupportedOperationException("Copy inference should only be done using copyin and copyout inferences"); //$NON-NLS-1$
	}
	
	public CopyInference(CopyinInference inferCopyin, CopyoutInference inferCopyout) {
		super(inferCopyin.construct);
		this.inferCopyin = inferCopyin;
		this.inferCopyout = inferCopyout;
		infer();
	}
	
	public CopyInference(CopyinInference inferCopyin, CopyoutInference inferCopyout, IASTStatement... accIgnore) {
		super(inferCopyin.construct, accIgnore);
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