package org.eclipse.ptp.pldt.openacc.internal.core.patternmatching;

/**
 * Simply an ArbitraryExpression that is an L-Value.
 * 
 * @author William Hester
 */
public class ArbitraryLValue extends ArbitraryExpression {
	
	private String id;
	
	public ArbitraryLValue(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public boolean isLValue() {
		return true;
	}
}
