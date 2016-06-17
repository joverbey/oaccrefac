package org.eclipse.ptp.pldt.openacc.core.transformations;

public class ZeroBasedStripMine extends ForLoopAlteration<StripMineCheck> {

	private int stripFactor;
	private boolean handleOverflow;
	private String newNameOuter;
	private String newNameInner;
	
	public ZeroBasedStripMine(IASTRewrite rewriter, int stripFactor, boolean handleOverflow, String newNameOuter, String newNameInner, StripMineCheck check) {
		super(rewriter, check);
		this.stripFactor = stripFactor;
		this.handleOverflow = handleOverflow;
		this.newNameOuter = newNameOuter;
		this.newNameInner = newNameInner;
	}
	
	@Override
	protected void doChange() {

	}

}
