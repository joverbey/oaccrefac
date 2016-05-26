package org.eclipse.ptp.pldt.openacc.core.dataflow;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;

public class InferCopy extends InferDataTransfer {
	
	Map<IASTStatement, Set<IBinding>> copyin;
	Map<IASTStatement, Set<IBinding>> copyout;
	
	public InferCopy(ReachingDefinitions rd, IASTStatement... construct) {
		throw new IllegalStateException("Copy inference should only be done using copyin and copyout inferences");
	}
	
	public InferCopy(IASTStatement... construct) {
		throw new IllegalStateException("Copy inference should only be done using copyin and copyout inferences");
	}
	
	public InferCopy(InferCopyin inferCopyin, InferCopyout inferCopyout) {
		super(null, inferCopyin.construct);
		this.copyin = inferCopyin.get();
		this.copyout = inferCopyout.get();
		infer();
	}

	@Override
	protected void infer() {
		for(IASTStatement construct : copyin.keySet()) {
    		if(copyout.containsKey(construct)) {
    			Set<IBinding> ins = copyin.get(construct);
    			Set<IBinding> outs = copyout.get(construct);
    			for(IBinding inVar : new HashSet<IBinding>(ins)) {
    				for(IBinding outVar : new HashSet<IBinding>(outs)) {
    					if(inVar.equals(outVar)) {
    						ins.remove(inVar);
    						outs.remove(outVar);
    						if(transfers.containsKey(construct)) {
    							transfers.get(construct).add(inVar);
    						}
    						else {
    							transfers.put(construct, new TreeSet<IBinding>());
    							transfers.get(construct).add(inVar);
    						}
    					}
    				}
    			}
    		}
    	}
	}
	
}